namespace java com.twitter.diffy.thriftjava
#@namespace scala com.twitter.diffy.thriftscala

struct Responses {
  # Responses from each of the different instances in JSON format
  1: string candidate
  2: string primary
  3: string secondary
}

struct DifferenceResult {
  # Unique id of the request, either the trace id or a randomly generated long
  1: i64 id,

  # Trace id of the request, if present
  2: optional i64 trace_id,

  # Endpoint e.g. "get", "get_by_id"
  3: string endpoint,

  # Unix timestamp in milliseconds
  4: i64 timestamp_msec,

  # Map from field -> difference JSON string
  # e.g. "user/profile/name" -> "{type: 'PrimitiveDifference', left:'Foo', right:'Bar'}"
  5: map<string, string> differences,

  # JSON representation of the incoming request
  6: string request,

  # Responses from primary, secondary and candidate
  7: Responses responses
}

service SampleService {
  string get(
    1: string request
  )
}