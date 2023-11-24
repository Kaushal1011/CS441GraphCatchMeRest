# CS 441 Catch Me Game ReadMe



##  CS 441 Catch Me Game (Rest API )



Sources in the sample:

### Graph Catch Me Rest Package

* `QuickstartApp.scala` -- contains the main method which bootstraps the application
* `CatchMeRoutes.scala` -- Akka HTTP `routes` defining exposed endpoints
* `CatchMeGameRegistry.scala` -- the actor which handles the requests and keeps the in-memory database
* `JsonFormats.scala` -- converts the JSON data from requests into Scala types and from Scala types into JSON responses

### Graph Catch Me Game Package

* `CatchMeGameEnvironment.scala` -- contains the methods for the environment of the game as inspired from gym environments

### Helpers Package

* `NodeDataParser.scala` -- contains the methods for parsing the data from the nodes
* `LamdaInvoker.scala` -- contains the methods for invoking the lambda functions using protobuf