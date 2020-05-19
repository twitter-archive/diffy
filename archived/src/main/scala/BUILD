scala_library(
  name='scala',
  dependencies=[
    'diffy/src/main/scala/com/twitter/diffy',
    'diffy/src/main/webapp'
  ],
  resources=[
    'diffy/src/main/resources'
  ]
)

jvm_binary(
  name='bin',
  basename='diffy-server',
  main='com.twitter.diffy.Main',
  dependencies=[':scala']
)