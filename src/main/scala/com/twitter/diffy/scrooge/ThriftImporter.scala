package com.twitter.diffy.scrooge

import com.twitter.scrooge.frontend.{DirImporter, Importer}
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile
import scala.collection.JavaConversions._
import scala.io.Source

object ZippedFileImporter {
  def apply(zipFiles: Seq[ZipFile]): Importer = {
    val thriftDir = Files.createTempDirectory("thrift-")
    thriftDir.toFile.deleteOnExit()

    zipFiles foreach { zipFile =>
      zipFile.entries.toList.collect {
        case zipEntry if !zipEntry.isDirectory && zipEntry.getName.endsWith(".thrift") =>
          val data = Source.fromInputStream(zipFile.getInputStream(zipEntry), "UTF-8").mkString

          val newFile = new File(thriftDir.toString + File.separator + zipEntry.getName)
          new File(newFile.getParent).mkdirs()

          Files.write(newFile.toPath, data.getBytes)
      }
    }

    DirImporter(thriftDir.toFile)
  }
}

object FileImporter {
  def apply(files: Seq[File]): Importer = {
    val thriftDir = Files.createTempDirectory("thrift-")
    thriftDir.toFile.deleteOnExit()

    files foreach { file =>
      val newFile = new File(thriftDir.toString + File.separator + file.getName)
      Files.copy(file.toPath, newFile.toPath)
    }

    DirImporter(thriftDir.toFile)
  }
}
