package com.twitter.diffy.lifter

import com.twitter.scrooge.ast._
import com.twitter.scrooge.frontend.{Importer, ResolvedDocument, ThriftParser, TypeResolver}
import com.twitter.util.{Future, Memoize, NoStacktrace, Try}
import org.apache.thrift.protocol._
import org.apache.thrift.TApplicationException
import org.apache.thrift.transport.{TMemoryInputTransport, TTransport}

object ThriftLifter {
  private[lifter] val FileNameRegex = ".*?([^/]+)\\.[^/]+$".r

  case class InvalidMessageTypeException(messageType: Byte)
    extends RuntimeException("Invalid message type")
    with NoStacktrace

  case class MethodNotFoundException(method: String)
    extends RuntimeException("Method not found: %s".format(method))
    with NoStacktrace

  case class FieldOutOfBoundsException(id: Int)
    extends RuntimeException("Field out of bounds: %d".format(id))
    with NoStacktrace

  case class UnexpectedThriftTypeException(thriftType: String)
    extends RuntimeException("Unexpected thrift type: %s".format(thriftType))
    with NoStacktrace

  case class InvalidServiceException(service: String)
    extends RuntimeException("Invalid service: %s".format(service))
    with NoStacktrace

  private[this] def filename(path: String): Option[String] =
    path match {
      case FileNameRegex(name) => Some(name)
      case _ => None
    }

  def fromImporter(importer: Importer, thriftFiles: Seq[String], serviceName: String): ThriftLifter = {
    val parser = new ThriftParser(importer, false, false, false)
    val resolver = TypeResolver()
    val resolvedServices: Seq[(ResolvedDocument, Service)] =
      thriftFiles flatMap { file: String =>
        val doc = parser.parseFile(file)
        val resolvedDocument = resolver(doc)
        resolvedDocument.document.services.map { svc =>
          (resolvedDocument, svc)
        }
      }
    resolvedServices.find { case (_, svc) => svc.sid.name == serviceName } match {
      case Some((rdoc, service)) =>
        val inheritedMethods = rdoc.collectParentServices(service) flatMap {
          case (_, svc) => svc.functions
        }
        new ThriftLifter(service, inheritedMethods)
      case None => throw InvalidServiceException(serviceName)
    }
  }
}

class ThriftLifter(
    service: Service,
    inheritedMethods: Seq[Function],
    protocolFactory: TProtocolFactory = new TBinaryProtocol.Factory())
  extends MapLifter
{
  import ThriftLifter._

  def apply(input: Array[Byte]): Future[Message] = {
    val buffer = new TMemoryInputTransport(input)
    Future.const(apply(buffer))
  }

  def apply(transport: TTransport): Try[Message] =
    apply(protocolFactory.getProtocol(transport))

  def apply(proto: TProtocol): Try[Message] = Try { readMessage(proto) }

  def readMessage(proto: TProtocol): Message = {
    val msg = proto.readMessageBegin()
    val result = msg.`type` match {
      case TMessageType.EXCEPTION =>
        val exception = TApplicationException.read(proto)
        proto.readMessageEnd()
        throw exception

      case mType =>
        readMethod(proto, msg.name, mType)
    }
    proto.readMessageEnd()
    result
  }

  def readMethod(proto: TProtocol, methodName: String, methodType: Byte): Message = {
    val method = findMethod(methodName)
    proto.readStructBegin()
    val result = methodType match {
      case TMessageType.CALL | TMessageType.ONEWAY =>
        Message(Some(methodName), readMethodCall(proto, method))
      case TMessageType.REPLY =>
        Message(Some(methodName), readMethodResponse(proto, method))
      case _ => throw InvalidMessageTypeException(methodType)
    }
    proto.readStructEnd()
    result
  }

  private[this] def findMethod(method: String): Function =
    findMethodSignature(method)

  private[lifter] val findMethodSignature: (String => Function) =
    Memoize { meth: String =>
      service.functions.find(_.funcName.name == meth) match {
        case Some(function) => function
        case None =>
          inheritedMethods.find(_.funcName.name == meth).getOrElse(
            throw MethodNotFoundException(meth))
      }
    }

  def readFields(proto: TProtocol)(fieldReader: TField => Map[String, Any]): FieldMap[Any] = {
    val field = proto.readFieldBegin()
    FieldMap(field.`type` match {
      case TType.STOP => Map.empty[String, Any]
      case ft =>
        val fieldMap = fieldReader(field)
        proto.readFieldEnd()
        fieldMap ++ readFields(proto)(fieldReader).toMap
    })
  }

  def readMethodCall(proto: TProtocol, method: Function): FieldMap[Any] =
    readFields(proto) { field =>
      val index = math.max(0, field.id - 1)
      val arg = method.args(index)
      Map(arg.originalName -> readType(proto, arg.fieldType))
    }

  def readMethodResponse(proto: TProtocol, method: Function): FieldMap[Any] =
    readFields(proto) { field =>
      field.id match {
        case 0 => Map("success" -> readType(proto, method.funcType))
        case i =>
          val throwField = method.throws(i - 1)
          Map(throwField.originalName -> readType(proto, throwField.fieldType))
      }
  }

  def readType(proto: TProtocol, funcType: FunctionType): Any =
    funcType match {
      case Void | OnewayVoid => ()
      case TBool => proto.readBool()
      case TByte => proto.readByte()
      case TI16 => proto.readI16()
      case TI32 => proto.readI32()
      case TI64 => proto.readI64()
      case TDouble => proto.readDouble()
      case TString => proto.readString()
      case TBinary => proto.readBinary()
      case struct: StructType => readStruct(proto, struct)
      case enum: EnumType => readEnum(proto, enum)
      case map: MapType => readMap(proto, map)
      case set: SetType => readSet(proto, set)
      case list: ListType => readList(proto, list)
      case _: NamedType => throw UnexpectedThriftTypeException("NamedType")
      case _: ReferenceType => throw UnexpectedThriftTypeException("ReferenceType")
    }

  def readStruct(proto: TProtocol, structType: StructType): FieldMap[Any] = {
    proto.readStructBegin()
    readFields(proto) { field =>
      structType.struct.fields.find(_.index == field.id) match {
        case Some(f) => Map(f.originalName -> readType(proto, f.fieldType))
        case None => throw FieldOutOfBoundsException(field.id)
      }
    }
  }

  def readList(proto: TProtocol, listType: ListType): Seq[Any] = {
    val listHeader = proto.readListBegin()
    val result =
      (0 until listHeader.size) map { _ =>
        readType(proto, listType.eltType)
      }
    proto.readListEnd()
    result.toSeq
  }

  def readMap(proto: TProtocol, mapType: MapType): Map[Any, Any] = {
    val mapHeader = proto.readMapBegin()
    val result =
      ((0 until mapHeader.size) map { _ =>
        readType(proto, mapType.keyType) -> readType(proto, mapType.valueType)
      }).toMap
    proto.readMapEnd()
    result
  }

  def readSet(proto: TProtocol, setType: SetType): Set[Any] = {
    val setHeader = proto.readSetBegin()
    val result =
      ((0 until setHeader.size) map { _ =>
        readType(proto, setType.eltType)
      }).toSet
    proto.readSetEnd()
    result
  }

  def readEnum(proto: TProtocol, enumType: EnumType) = {
    val enumValue = proto.readI32()
    val enum = enumType.enum.values.find(_.value == enumValue)
    enum.map(_.sid.name).getOrElse("unknown (%d)".format(enumValue))
  }

  private def typeToHuman(byte: Byte) =
    byte match {
      case TType.STOP   => "STOP"
      case TType.VOID   => "VOID"
      case TType.BOOL   => "BOOL"
      case TType.BYTE   => "BYTE"
      case TType.DOUBLE => "DOUBLE"
      case TType.I16    => "I16"
      case TType.I32    => "I32"
      case TType.I64    => "I64"
      case TType.STRING => "STRING"
      case TType.STRUCT => "STRUCT"
      case TType.MAP    => "MAP"
      case TType.SET    => "SET"
      case TType.LIST   => "LIST"
      case TType.ENUM   => "ENUM"
      case _            => "UNKNOWN (" + byte + ")"
    }

}
