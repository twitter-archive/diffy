target(
  name='diffy',
  dependencies=[
    'diffy/src/main/scala',
  ]
)

target(
  name='tests',
  dependencies=[
    'diffy/src/test/scala',
  ]
)

target(
  name='bin',
  dependencies=['diffy/src/main/scala:bin']
)

jvm_app(
  name='bundle',
  basename='diffy-package-dist',
  binary='diffy/src/main/scala:bin',
  bundles=[
    bundle(fileset=globs('config/*'))
  ]
)
