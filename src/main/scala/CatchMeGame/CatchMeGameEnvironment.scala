package CatchMeGame

import com.google.common.graph.{GraphBuilder, MutableGraph}
import helpers.NodeDataParser

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.io.Source
import scala.util.Using

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors


// Enumeration for the game state
object GameState extends Enumeration {
  val Start, Ongoing, Finished = Value
}

// Enumeration for the winner
object Winner extends Enumeration {
  val Police, Thief, NoOne = Value
}

import scala.util.Random

// Case classes for the game environment and game running
case class ComparableNode(
                           id: Int = -1,
                           props: List[Int] = List.empty,
                           childPropsHash: List[Int] = List.empty,
                           valuableFlag: Boolean = false
                         )
case class ConfidenceScore(
                            id: Int = -1,
                            score: Double = 0.0
                          )

case class AgentData(
                    name:String,
                    currentLocation:ComparableNode,
                    adjacentNodes:List[ComparableNode],
                    confidenceScores: List[ConfidenceScore] = List.empty,
                    valuableDataDistance: Int = -1
                    )

case class GameState(policeLoc: Int, thiefLoc: Int, winner: String)



// Environment class defined as a case class for immutability
case class CatchMeGameEnvironment(
                        regionalGraphPath: Option[String] = None,
                        queryGraphPath: Option[String] = None,
                        regionalGraph: MutableGraph[ComparableNode] = GraphBuilder.undirected().build[ComparableNode](),
                        queryGraph: MutableGraph[ComparableNode] = GraphBuilder.undirected().build[ComparableNode](),
                        currentPoliceNode: ComparableNode = ComparableNode(-1, List.empty, List.empty, false),
                        currentThiefNode: ComparableNode = ComparableNode(-1, List.empty, List.empty, false),
                        gameState: GameState.Value = GameState.Start,
                        winner: Option[Winner.Value] = None
                      ) {

  // Actor system context for logging and fetching config
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "CatchMeGameEnvironment")


  // Methods

  // A helper method to load a graph from a file
  // can load from file, can load from url
  private def loadGraph(graphPath: String): MutableGraph[ComparableNode] = {
    val graph = GraphBuilder.undirected().build[ComparableNode]()
    val nodeMap = scala.collection.mutable.Map[Int, ComparableNode]()

    def getNode(id: Int, props: List[Int], childrenPropsHash: List[Int], valuable: Boolean): ComparableNode = {
      nodeMap.getOrElseUpdate(id, ComparableNode(id, props, childrenPropsHash, valuable))
    }

    Using(if (graphPath.startsWith("http://") || graphPath.startsWith("https://")) {
      Source.fromURL(graphPath)
    } else {
      Source.fromFile(graphPath)
    }) { source =>
      source.getLines().foreach { line =>
        val edge = NodeDataParser.parseEdgeData(line)
        val srcNode = getNode(edge.srcId, edge.propertiesSrc, edge.children_prop_hash_destination, edge.valuableSrc)
        val dstNode = getNode(edge.dstId, edge.propertiesDst, edge.children_prop_hash_destination, edge.valuableDst)
        graph.putEdge(srcNode, dstNode)
      }
    } match {
      case scala.util.Success(_) => graph
      case scala.util.Failure(ex) =>
        println(s"Failed to load graph: ${ex.getMessage}")
        GraphBuilder.undirected().build[ComparableNode]() // Return an empty graph or handle the error as appropriate
    }
  }

  // A helper method to load the regional graph
  private def loadRegionalGraph(graphPath:String): CatchMeGameEnvironment = {
    this.copy(
      regionalGraphPath = Some(graphPath),
      regionalGraph = loadGraph(graphPath)
    )
  }

  // A helper method to load the query graph
  private def loadQueryGraph(nodesPath: String): CatchMeGameEnvironment = {
    this.copy(
      queryGraphPath = Some(nodesPath),
      queryGraph = loadGraph(nodesPath)
    )
  }

  // A helper method to find the distance to the nearest valuable data
  private def findDistanceToValuableData(graph: MutableGraph[ComparableNode], startId: Int): Option[Int] = {
    // A helper method to find a node by id
    def findNode(graph: MutableGraph[ComparableNode], nodeId: Int): Option[ComparableNode] =
      graph.nodes().asScala.find(_.id == nodeId)

    // Tail-recursive BFS implementation
    @tailrec
    def bfs(frontier: List[(ComparableNode, Int)], visited: Set[Int]): Option[Int] = {
//      println(s"Current frontier: ${frontier.map(_._1.id)}") // Debug: Print the current frontier
      frontier match {
        case (node, dist) :: rest =>
//          println(s"Visiting node: ${node.id}") // Debug: Print the current node being visited
          if (node.valuableFlag) {
//            println(s"Found valuable node at distance $dist") // Debug: Print when a valuable node is found
            Some(dist)
          } else {
            val neighbors = graph.adjacentNodes(node).iterator().asScala
              .filterNot(n => visited.contains(n.id))
              .toList.map(n => (n, dist + 1))
//            println(s"Neighbors to be added: ${neighbors.map(_._1.id)}") // Debug: Print the neighbors to be added
            bfs(rest ++ neighbors, visited + node.id)
          }
        case Nil =>
//          println("No more nodes in frontier, terminating BFS") // Debug: Print when the frontier is empty
          None
      }
    }

    findNode(graph, startId) match {
      case Some(startNode) =>
//        println(s"Starting BFS from node: ${startNode.id}") // Debug: Print the start node for BFS
        bfs(List((startNode, 0)), Set(startId))
      case None =>
//        println(s"No node found with ID $startId") // Debug: Print if the start node is not found
        None
    }
  }

  // A helper method to find the confidence score for each neighbour
  private def getConfidenceScore(agentName: String): List[ConfidenceScore] = {
    val environment = this
    val neighbours = if (agentName == "police") {
      val currentNodeId = environment.currentPoliceNode.id
      val matchingNode = environment.queryGraph.nodes().asScala.find(node => node.id == currentNodeId).getOrElse(ComparableNode(-1, List.empty, List.empty))
      environment.queryGraph.adjacentNodes(matchingNode).asScala.toList
    } else if (agentName == "thief") {
      val currentNodeId = environment.currentThiefNode.id
      val matchingNode = environment.queryGraph.nodes().asScala.find(node => node.id == currentNodeId).getOrElse(ComparableNode(-1, List.empty, List.empty))
      environment.queryGraph.adjacentNodes(matchingNode).asScala.toList
    } else {
      system.log.error("Invalid agent name")
      List.empty
    }
    val queryGraphEdgeSize = environment.queryGraph.edges().size()

    val confidenceScores = neighbours.map(node => {
      val regionalGraphEdgeSize = environment.regionalGraph.edges().size().toDouble
      val score = if (queryGraphEdgeSize > 0) {
        regionalGraphEdgeSize / queryGraphEdgeSize.toDouble
      } else {
        // Handle the case when queryGraphEdgeSize is 0
        // This could be a default value or a specific logic as per your application's need
        0.0 // or any other default value
      }
      ConfidenceScore(node.id, score)
    })
    confidenceScores
  }


  // A helper method to check if the game is over
  private def checkGameOverWinner(): Winner.Value = {
    // Implement the logic to check if the game is over
    // Return true if the game is over, false otherwise
    // The game is over if the police and thief are in the same node
    // or if the thief is in a node that has no neighbours
    val environment = this

    if (environment.currentPoliceNode.id == environment.currentThiefNode.id){
      Winner.Police
    }
    // if thief is on a node with valuable data
    else if (environment.currentThiefNode.valuableFlag){
      Winner.Thief
    }
    else if (environment.regionalGraph.adjacentNodes(environment.currentThiefNode).isEmpty){
       Winner.Police
    }
    else if (environment.regionalGraph.adjacentNodes(environment.currentPoliceNode).isEmpty){
       Winner.Thief
    }
    else{
       Winner.NoOne
    }

  }

  // A helper method to get the agent data
  def getAgentData(agentName:String): AgentData = {
    try {
      val environment = this
      if (agentName == "police") {
        val currentNodeId = environment.currentPoliceNode.id
        val matchingNode = environment.queryGraph.nodes().asScala.find(node => node.id == currentNodeId).getOrElse(ComparableNode(-1, List.empty, List.empty))
        val neighbours = environment.queryGraph.adjacentNodes(matchingNode).asScala.toList

        AgentData(agentName, environment.currentPoliceNode, neighbours, getConfidenceScore(agentName), findDistanceToValuableData(environment.regionalGraph, environment.currentPoliceNode.id).getOrElse(-1))
      } else if (agentName == "thief") {
        val currentNodeId = environment.currentThiefNode.id
        val matchingNode = environment.queryGraph.nodes().asScala.find(node => node.id == currentNodeId).getOrElse(ComparableNode(-1, List.empty, List.empty))
        val neighbours = environment.queryGraph.adjacentNodes(matchingNode).asScala.toList
        AgentData(agentName, environment.currentThiefNode, neighbours, getConfidenceScore(agentName), findDistanceToValuableData(environment.regionalGraph, environment.currentThiefNode.id).getOrElse(-1))
      } else {
        system.log.error("Invalid agent name")
        AgentData("null", ComparableNode(-1, List.empty, List.empty, false), List.empty, List.empty, 0)
      }
    }
    catch {
      case e: Exception => {
        system.log.error("Error getting agent data");
        AgentData("null", ComparableNode(-1, List.empty, List.empty, false), List.empty, List.empty, 0)
      }
    }
  }

  // do an action on the environment
  def action(agentName: String, moveToNeighbourId: Int): CatchMeGameEnvironment = {
    try {
      // Implement the logic to move the agent (police or thief) to the given neighbour node
      // Update the currentPoliceNode or currentThiefNode accordingly
      // Return a new Environment with the updated node
      val environment = this

      if (agentName == "police") {

        // check if moveToNeighbourId is a neighbour of currentPoliceNode
        val neighbours = environment.regionalGraph.adjacentNodes(environment.currentPoliceNode).asScala.toList
        val moveToNeighbour: ComparableNode = neighbours.find(node => node.id == moveToNeighbourId).getOrElse(ComparableNode(-1, List.empty, List.empty, false))
        if (moveToNeighbour.id != -1) {
          val winner = environment.checkGameOverWinner()
          if (winner != Winner.NoOne) {
            environment.copy(currentPoliceNode = moveToNeighbour, gameState = GameState.Finished, winner = Some(winner))
          } else {
            environment.copy(currentPoliceNode = moveToNeighbour)
          }

        } else {
          // invalid neighbour provided for agent
          // if loss flag is set then agent loses
          // else no op

          system.log.error("Invalid neighbour id provided police")

          val invalidMakesLoss = system.settings.config.getString("my-app.server.invalid-makes-loss")

          if (invalidMakesLoss == "true") {
            environment.copy(gameState = GameState.Finished, winner = Some(Winner.Thief))
          } else {
            environment
          }
        }


      } else if (agentName == "thief") {

        // check if moveToNeighbourId is a neighbour of currentThiefNode
        val neighbours = environment.regionalGraph.adjacentNodes(environment.currentThiefNode).asScala.toList
        val moveToNeighbour = neighbours.find(node => node.id == moveToNeighbourId.toInt).getOrElse(ComparableNode(-1, List.empty, List.empty, false))
        if (moveToNeighbour.id != -1) {
          val winner = environment.checkGameOverWinner()
          if (winner != Winner.NoOne) {
            environment.copy(currentThiefNode = moveToNeighbour, gameState = GameState.Finished, winner = Some(winner))
          } else {
            environment.copy(currentThiefNode = moveToNeighbour)
          }
        } else {
          // invalid neighbour provided for agent
          // if loss flag is set then agent loses
          // else no op

          system.log.error("Invalid neighbour id provided thief")

          val invalidMakesLoss = system.settings.config.getString("my-app.server.invalid-makes-loss")

          if (invalidMakesLoss == "true") {
            environment.copy(gameState = GameState.Finished, winner = Some(Winner.Police))
          } else {
            environment
          }
        }

      } else {
        system.log.error("Invalid agent name")
        environment

      }
    }
    catch {
      case e: Exception => {
        system.log.error("Error doing action graph");
        this
      }
    }

  }

  // init function take a regional graph and query graph and initialize the environment
  def init(regionalGraphPath: String, queryGraphPath: String): CatchMeGameEnvironment = {
    try {


      val loadedEnvironment = loadRegionalGraph(regionalGraphPath)
        .loadQueryGraph(queryGraphPath)
        .copy(gameState = GameState.Ongoing)

      // Assuming that the regional graph is where the game takes place
      val regionalNodes = loadedEnvironment.regionalGraph.nodes()
      val allNodes = regionalNodes.asScala.toList
      val allNodesQuery = loadedEnvironment.queryGraph.nodes().asScala.toList

      val intersectionRegionalQuery = allNodes.intersect(allNodesQuery)

      // Picking random nodes for police and thief
      val random = new Random()
      val randomPoliceNode = intersectionRegionalQuery(random.nextInt(intersectionRegionalQuery.size))
      val randomThiefNode = intersectionRegionalQuery(random.nextInt(intersectionRegionalQuery.size))

      loadedEnvironment.copy(
        currentPoliceNode = randomPoliceNode,
        currentThiefNode = randomThiefNode,
        winner = Some(Winner.NoOne)
      )
    }
    catch {
      case e: Exception => {
        system.log.error("Error loading graph");
        this
      }
    }

  }

  // reset function resets the environment on the same graphs
  def reset(): CatchMeGameEnvironment = {
    // Reset the game state and graphs
    val resetEnvironment = this.copy(
      gameState = GameState.Ongoing,
    )

    // Assuming we want to pick new random locations after reset
    val regionalNodes = resetEnvironment.regionalGraph.nodes().asInstanceOf[java.util.Set[ComparableNode]]
    val allNodes = regionalNodes.asScala.toList

    // Picking random nodes for police and thief
    val random = new Random()
    val randomPoliceNode = allNodes(random.nextInt(allNodes.size))
    val randomThiefNode = allNodes(random.nextInt(allNodes.size))

    resetEnvironment.copy(
      currentPoliceNode = randomPoliceNode,
      currentThiefNode = randomThiefNode,
      winner = Some(Winner.NoOne)
    )
  }

  // reset function resets the environment on the new provided graphs
  def reset(regionalGraphPath: String, queryGraphPath: String): CatchMeGameEnvironment = {
    try {
      val resetEnvironment = loadRegionalGraph(regionalGraphPath)
        .loadQueryGraph(queryGraphPath)
        .copy(gameState = GameState.Ongoing)

      // Assuming we want to pick new random locations after reset
      val regionalNodes = resetEnvironment.regionalGraph.nodes().asInstanceOf[java.util.Set[ComparableNode]]
      val allNodes = regionalNodes.asScala.toList

      // Picking random nodes for police and thief
      val random = new Random()
      val randomPoliceNode = allNodes(random.nextInt(allNodes.size))
      val randomThiefNode = allNodes(random.nextInt(allNodes.size))

      resetEnvironment.copy(
        currentPoliceNode = randomPoliceNode,
        currentThiefNode = randomThiefNode,
        winner = Some(Winner.NoOne)
      )
    }
    catch {
      case e: Exception => {
        system.log.error("Error loading graph");
        this
      }
    }
  }

}

// Singleton object to run the game environment
//object CatchMeGameRunner {
//
//  def main(args: Array[String]): Unit = {
//    // Initialize the environment with some paths (you need to replace these with actual paths or test data)
//    val regionalGraphTestPath = "./input/edges.txt" // Replace with actual file path
//    val queryGraphTestPath = "./input/edgesPerturbed.txt" // Replace with actual file path
//
//    // Initializing the game environment with test paths
//    val environment = CatchMeGameEnvironment().init(regionalGraphTestPath, queryGraphTestPath)
//
//    // Print out the initial state
//    println(s"Initial Police Node: ${environment.currentPoliceNode}")
//    println(s"Initial Thief Node: ${environment.currentThiefNode}")
//    println(s"Game State: ${environment.gameState}")
//    println(s"Winner: ${environment.winner.getOrElse("No winner yet")}")
//
//    // Perform some actions to test functionality
//    // You need to replace 'moveToNeighbourId' with an actual ID based on your test data
//    val moveToNeighbourId = environment.getAgentData("police").adjacentNodes.head.id
//    val actionTestEnvironment = environment.action("police", moveToNeighbourId)
//
//    // Print out the state after action
//    println(s"After action Police Node: ${actionTestEnvironment.currentPoliceNode}")
//    println(s"After action Thief Node: ${actionTestEnvironment.currentThiefNode}")
//    println(s"After action Game State: ${actionTestEnvironment.gameState}")
//    println(s"After action Winner: ${actionTestEnvironment.winner.getOrElse("No winner yet")}")
//
//    // Reset the game environment and test paths
//    val resetEnvironment = actionTestEnvironment.reset(regionalGraphTestPath, queryGraphTestPath)
//
//    // Print out the state after reset
//    println(s"After reset Police Node: ${resetEnvironment.currentPoliceNode}")
//    println(s"After reset Thief Node: ${resetEnvironment.currentThiefNode}")
//    println(s"After reset Game State: ${resetEnvironment.gameState}")
//    println(s"After reset Winner: ${resetEnvironment.winner.getOrElse("No winner yet")}")
//  }
//}