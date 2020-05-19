module = angular.module 'diffy', [], ($interpolateProvider, $locationProvider) ->
  $interpolateProvider.startSymbol '[['
  $interpolateProvider.endSymbol ']]'
  $locationProvider.html5Mode false

module.service 'router', ($timeout, $rootScope) ->
  router = new Grapnel()
  routes = []
  (route, callback) ->
    router.add route, (req) ->
      $timeout ->
        callback req
        $rootScope.$apply()

module.factory 'endpointInclusions', -> {}
module.factory 'info', -> {}
module.factory 'percentageRating', ->
  (percent) ->
    if percent < .1 then "perfect"
    else if percent < 5 then "almost"
    else if percent < 50 then "neutral"
    else "poor"

module.filter 'ago', ->
  (input) ->
    if input <= 0 then "forever" else moment(input).fromNow()

module.filter 'formatDate', ->
  (input) ->
    return "-" if input <= 0
    moment(input).format("lll")

module.directive 'ngOnEsc', ->
  (scope, element, attrs) ->
    $(document).bind 'keydown keypress', (event) ->
      if event.keyCode == 27
        scope.$eval(attrs.ngOnEsc)
        scope.$apply()
        event.preventDefault()

module.directive 'deepLink', ($rootScope) ->
  (scope, element, attrs) ->
    element.bind 'click', (event) ->
      link = scope.$eval attrs.deepLink
      link = link.join("/") if $.isArray link
      window.location = '#/' + link

module.directive 'requestTreeHighlight', ($rootScope, $timeout) ->
  (scope, element, attrs) ->
    scope.$root.$on "highlight_differences", (event, pathSelectors) ->
      $timeout ->
        $rootScope.$apply()
        for i, path of pathSelectors
          jQuery(path + " strong").addClass('difference')
      , 0

module.service 'api', ($rootScope) ->
  (url, params, success) ->
    params = $.extend params, {}
    $.get apiRoot + "/api/1/" + url, params, ->
      success.apply(this, arguments)
      $rootScope.$apply()
    .fail ->
      console.log arguments

class Metadata
  constructor: ->
    @metadata = {}
    @defaults = {}
    @prefix = -> "default"
  get: (key, prefix = @prefix()) ->
    @metadata[prefix] = {} if @metadata[prefix] == undefined
    @metadata[prefix][key] = ($.extend true, {}, @defaults) if @metadata[prefix][key] == undefined
      # create a copy of defaults
    @metadata[prefix][key]
  each: (func) ->
    for key, value of @metadata[@prefix()]
      func(key, value)

module.factory 'globalFields', -> {}
module.factory 'globalMetadata', -> new Metadata()

module.controller 'EndpointController', ($scope, $interval, api, router, info, globalMetadata, globalFields, endpointInclusions, percentageRating) ->
  $scope.globalExclusion = false
  $scope.endpoints = {}
  $scope.sortedEndpoints = []
  $scope.metadata = new Metadata()
  $scope.percentageRating = percentageRating

  $scope.inclusionPercentage = (ep) ->
    if ep of endpointInclusions then endpointInclusions[ep]
    else 1

  $scope.info = info
  metadata = $scope.metadata
  metadata.defaults.selected = false

  router '/ep/:endpoint/:path?/:id?', (req) ->
    metadata.each (key, item) ->
      item.selected = false if item.selected
    metadata.get(req.params.endpoint).selected = true

  $scope.size = Object.size

  $scope.$root.$on 'exclusions_updated', (event) ->
    sortEndpoints()

  $scope.showSettings = ->
    $scope.$root.$emit 'show_settings'

  $scope.loadEndpoints = ->
    api "endpoints", {}, (endpoints) ->
      if !_.isEqual(endpoints, $scope.endpoints)
        endpointObjs = _.map endpoints, (stats, endpoint) ->
          {
            name: endpoint
            failureRate: stats.differences / stats.total
            inclusion: $scope.inclusionPercentage(endpoint)
            stats: stats
          }
        $scope.endpoints = endpointObjs
        sortEndpoints()

  sortEndpoints = ->
    $scope.endpoints.sort (a, b) ->
      aInclusion = $scope.inclusionPercentage(a.name)
      bInclusion = $scope.inclusionPercentage(b.name)
      if (a.failureRate * aInclusion > b.failureRate * bInclusion) then -1
      else if (a.failureRate * aInclusion < b.failureRate * bInclusion) then 1
      else if (a.name < b.name) then -1
      else 1

  $scope.excludeAll = ->
    endpoints = []
    for i, endpoint of $scope.endpoints
      endpoints.push(endpoint.name)

    $scope.$root.$emit 'toggle_global_exclusion', endpoints, $scope.globalExclusion

  $scope.apiInterval = $interval $scope.loadEndpoints, 2000
  $scope.loadEndpoints()


module.controller 'FieldsController', ($scope, api, router, globalMetadata, globalFields, endpointInclusions, percentageRating) ->
  $scope.loading = false
  $scope.percentageRating = percentageRating

  metadata = globalMetadata
  metadata.defaults.included = true

  metadata.prefix = -> $scope.endpointName

  router '/ep/:endpoint/:path?/:id?', (req) ->
    if $scope.endpointName != req.params.endpoint
      clearInterval $scope.loadFieldsInterval
      $scope.endpointName = req.params.endpoint
      $scope.loadFields false
      $scope.loadFieldsInterval = setInterval ->
        $scope.loadFields true
      , 5000

  $scope.expandFields = (fields, endpointName) ->
    obj = {}
    for path, item of fields
      $scope.addField obj, path, item, endpointName
    obj

  $scope.iterateFields = (field, func, endpointName) ->
    iterate = (children) ->
      for name, child of children
        func child, metadata.get(child.path, endpointName)
        iterate child.children if child.children
    iterate field

  $scope.addField = (obj, path, item, endpointName) ->
    path = path.split "." if typeof path == "string"
    currentObj = obj
    currentPath = ""
    for fieldName, i in path
      currentPath += fieldName
      meta = metadata.get(currentPath, endpointName)
      if i < path.length - 1
        currentObj[fieldName] = currentObj[fieldName] || {path: currentPath, children: {}}
        meta.differences = 0 if !meta.differences
        currentObj[fieldName].path = currentPath
        currentObj = currentObj[fieldName].children
      else
        currentObj[fieldName] = $.extend currentObj[fieldName], { path: currentPath, terminal: true }, item
        meta.differences = item.differences
        meta.weight = item.weight
        meta.includedWeights = item.weight
        meta.noise = item.noise
      currentPath += "."

  $scope.expandAll = ->
    $scope.iterateFields $scope.fields, (field, meta) ->
        meta.collapsed = false

  $scope.clearExclusions = (endpointName) ->
    $scope.iterateFields globalFields[endpointName], (field, meta) ->
      meta.included = true
    , endpointName
    $scope.traverseFields(globalFields[endpointName], endpointName)

  $scope.inspectFields = (children, path, endpointName) ->
    meta = metadata.get(path, endpointName)
    meta.weight = 0
    meta.includedWeights = 0
    meta.childrenIncluded = false
    childrenHaveChildren = false
    for name, child of children
      childMeta = metadata.get(child.path, endpointName)
      if child.children
        childrenHaveChildren = true
        $scope.inspectFields(child.children, child.path, endpointName) if !childMeta.terminal
        meta.childrenIncluded = true if childMeta.childrenIncluded && childMeta.included
      else
        meta.childrenIncluded = true if childMeta.included
      meta.weight += childMeta.weight
      meta.includedWeights += childMeta.includedWeights if childMeta.included
    meta.collapsed = !childrenHaveChildren if meta.collapsed == undefined

  $scope.traverseFields = (field, endpointName) ->
    $scope.inspectFields(field, undefined, endpointName)
    weight = 0
    includedWeights = 0
    for name, child of field
      meta = metadata.get(name, endpointName)
      weight += meta.weight
      includedWeights += meta.includedWeights if meta.included

    inclusionsPercentage = 0
    if includedWeights && weight
      inclusionsPercentage = (includedWeights) / (weight)
    if endpointName == $scope.endpointName
      $scope.diffs = Math.ceil(inclusionsPercentage * $scope.endpoint.differences)
      $scope.percentage = ((inclusionsPercentage * $scope.endpoint.differences) / $scope.endpoint.total) * 100
    if endpointInclusions[endpointName] != inclusionsPercentage
      endpointInclusions[endpointName] = inclusionsPercentage
      $scope.$root.$emit 'exclusions_updated'

  $scope.autoExclude = (endpointName) ->
    $scope.iterateFields globalFields[endpointName], (field, meta) ->
      if field.terminal
        meta.included = field.differences > field.noise && field.relative_difference > relativeThreshold && field.absolute_difference > absoluteThreshold
    , endpointName
    $scope.traverseFields(globalFields[endpointName], endpointName)

  $scope.collapseExcluded = ->
    $scope.iterateFields $scope.fields, (field, meta) ->
      if !meta.included || !meta.childrenIncluded
        meta.collapsed = true
    $scope.traverseFields($scope.fields, $scope.endpointName)

  $scope.size = Object.size
  $scope.pathSelected = (path) ->
    metadata.each (key, item) ->
      item.selected = false if item.selected
    metadata.get(path).selected = true
    $scope.$root.$emit 'load_path', $scope.endpointName, path
  $scope.hasFields = ->
    Object.size($scope.rawFields) > 0

  $scope.fields = null
  $scope.rawFields = null

  $scope.loadFields = (hideLoader) ->
    $scope.loading = !hideLoader
    if !hideLoader
      $scope.fields = null
      $scope.rawFields = null
    api 'endpoints/' + $scope.endpointName + '/stats', { "include_weights" : true, "exclude_noise" : excludeNoise }, (response) ->
      $scope.endpoint = response.endpoint
      $scope.loading = false
      if !_.isEqual response.fields, $scope.rawFields
        $scope.fields = $scope.expandFields response.fields, $scope.endpointName
        globalFields[$scope.endpointName] = $scope.fields
        $scope.traverseFields($scope.fields, $scope.endpointName)
      $scope.rawFields = response.fields

    $scope.getGlobalMetadata = (path) ->
      globalMetadata.get(path)

  $scope.$root.$on 'toggle_global_exclusion', (event, endpoints, globalExclusion) ->
    exclude = (endpointName) ->
      if globalExclusion
        $scope.autoExclude endpointName
      else
        $scope.clearExclusions endpointName

    loadAndExclude = (endpointName) ->
      api 'endpoints/' + endpointName + '/stats', { "include_weights" : true, "exclude_noise" : excludeNoise }, (response) ->
        globalFields[endpointName] = $scope.expandFields response.fields, endpointName
        $scope.traverseFields(globalFields[endpointName], endpointName)
        exclude(endpointName)

    for i, endpoint of endpoints
      if globalFields[endpoint] == undefined
        loadAndExclude(endpoint)
      else
        exclude(endpoint)


module.controller 'RequestsController', ($scope, api, router) ->
  $scope.loading = false

  router '/ep/:endpoint/:path/:id?', (req) ->
    if $scope.endpointName != req.params.endpoint || $scope.path != req.params.path
      $scope.path = req.params.path
      $scope.endpointName = req.params.endpoint
      $scope.loadPath req.params.endpoint, req.params.path, false

  $scope.loadPath = (endpoint, path, hideLoader) ->
    $scope.loading = !hideLoader
    $scope.requests = false
    $scope.path = path
    $scope.endpoint = endpoint
    api 'endpoints/' + endpoint + '/fields/' + path + '/results', {}, (response) ->
      $scope.loading = false
      for request in response.requests
        for path, difference of request.differences
          difference.collapsed = (path.indexOf $scope.path) != 0
      $scope.requests = response.requests

  $scope.requestSelected = (id) ->
    $scope.$root.$emit 'load_request', id

  $scope.size = Object.size
  $scope.countCollapsed = (diffs) ->
    i = 0
    for path, diff of diffs
      i++ if diff.collapsed
    i
  $scope.uncollapse = (diffs) ->
    for path, diff of diffs
      diff.collapsed = false

module.controller 'RequestController', ($scope, api, router) ->
  $scope.size = Object.size
  $scope.loading = false
  $scope.isObject = $.isPlainObject
  $scope.isArray = $.isArray
  $scope.requestId = false

  $scope.back = ->
    window.location = '#/ep/' + [$scope.endpointName, $scope.path].join('/')

  router '/ep/:endpoint/:path', (req) ->
    $scope.request = null

  router '/ep/:endpoint/:path/:id', (req) ->
    $scope.loading = true
    $scope.requestId = req.params.id
    $scope.endpointName = req.params.endpoint
    $scope.path = req.params.path
    api 'requests/' + $scope.requestId, {}, (response) ->
      $scope.loading = false
      $scope.request = response

      diffs = DeepDiff($scope.request.left, $scope.request.right)
      pathSelectors = []

      for i, diff of diffs
        pathSelectorArray = []
        for j, p of diff.path
          pathSelectorArray.push("[data-path='" + p + "']")
        pathSelector = pathSelectorArray.join(" ")
        pathSelectors.push(pathSelector)

      $scope.$root.$emit 'highlight_differences', pathSelectors

module.controller 'SettingsController', ($scope, $timeout, $interval, api, router, info) ->
  loadInfo = ->
    api 'info', {}, (response) ->
      $.extend info, response

  apiInterval = $interval loadInfo, 10000
  loadInfo()

  $scope.info = info

  $scope.visible = false
  $scope.$root.$on 'show_settings', (event) ->
    $scope.visible = true
    $scope.loading = true

  $scope.clear = ->
    $scope.clearLoading = true
    if confirm("This will reset all counters and clear all the saved requests/responses, are you sure?")
      api 'clear', {}, (response) ->
        $scope.clearLoading = true
        $timeout ->
          $scope.clearLoading = false
        , 2000

Object.size = (obj) ->
  size = 0
  for key of obj
    size++ if obj.hasOwnProperty(key)
  size
  