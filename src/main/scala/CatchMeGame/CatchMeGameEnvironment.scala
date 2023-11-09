package CatchMeGame

import com.google.common.graph.{GraphBuilder, MutableGraph}
import helpers.NodeDataParser
import scala.jdk.CollectionConverters._

import scala.io.Source
// Enumeration for the game state
object GameState extends Enumeration {
  val Start, Ongoing, Finished = Value
}

object Winner extends Enumeration {
  val Police, Thief, NoOne = Value
}

import scala.util.Random

case class ComparableNode(
                           id: Int = -1,
                           props: List[Int] = List.empty,
                           childPropsHash: List[Int] = List.empty,
                           valuableFlag: Boolean = false
                         )

case class AgentData(
                    name:String,
                    currentLocation:ComparableNode,
                    adjacentNodes:List[ComparableNode]
                    )

case class GameState(policeLoc: Int, thiefLoc: Int, winner: String)



// Environment class defined as a case class for immutability
case class CatchMeGameEnvironment(
                        regionalGraphPath: Option[String] = None,
                        queryGraphPath: Option[String] = None,
                        regionalGraph: MutableGraph[ComparableNode] = GraphBuilder.undirected().build[ComparableNode](),
                        queryGraph: MutableGraph[ComparableNode] = GraphBuilder.undirected().build[ComparableNode](),
                        currentPoliceNode: Option[ComparableNode] = None,
                        currentThiefNode: Option[ComparableNode] = None,
                        gameState: GameState.Value = GameState.Start,
                        winner: Option[Winner.Value] = None
                      ) {

  // Methods
   private def loadGraph(graphPath: String): MutableGraph[ComparableNode] = {

    val graph = GraphBuilder.undirected().build[ComparableNode]()

    val source: Source = if (graphPath.startsWith("http://") || graphPath.startsWith("https://")) {
      Source.fromURL(graphPath)
    } else {
      Source.fromFile(graphPath)
    }

    // Load edges
    source.getLines().foreach { line =>
      val edge = NodeDataParser.parseEdgeData(line)
      val srcNode = ComparableNode(edge.srcId, edge.propertiesSrc, edge.children_prop_hash_destination, edge.valuableSrc)
      val dstNode = ComparableNode(edge.dstId, edge.propertiesDst , edge.children_prop_hash_destination, edge.valuableDst)
      // this method silently creates nodes if they don't exist
      graph.putEdge(srcNode, dstNode)
    }

    graph
  }

  private def loadRegionalGraph(graphPath:String): CatchMeGameEnvironment = {
    this.copy(
      regionalGraphPath = Some(graphPath),
      regionalGraph = loadGraph(graphPath)
    )
  }

  private def loadQueryGraph(nodesPath: String): CatchMeGameEnvironment = {
    this.copy(
      queryGraphPath = Some(nodesPath),
      queryGraph = loadGraph(nodesPath)
    )
  }


  private def checkGameOverWinner(): Winner.Value = {
    // Implement the logic to check if the game is over
    // Return true if the game is over, false otherwise
    // The game is over if the police and thief are in the same node
    // or if the thief is in a node that has no neighbours
    val environment = this

    if (environment.currentPoliceNode.get.id == environment.currentThiefNode.get.id){
      Winner.Police
    }
    // if thief is on a node with valuable data
    else if (environment.currentThiefNode.get.valuableFlag){
      Winner.Thief
    }
    else if (environment.regionalGraph.adjacentNodes(environment.currentThiefNode.get).isEmpty){
       Winner.Police
    }
    else if (environment.regionalGraph.adjacentNodes(environment.currentPoliceNode.get).isEmpty){
       Winner.Thief
    }
    else{
       Winner.NoOne
    }

  }

  def getAgentData(agentName:String): AgentData = {
    val environment = this
    if (agentName=="police"){
      val neighbours = environment.regionalGraph.adjacentNodes(environment.currentPoliceNode.get).asScala.toList
      AgentData(agentName, environment.currentPoliceNode.get, neighbours)
    } else if(agentName=="thief"){
      val neighbours = environment.regionalGraph.adjacentNodes(environment.currentThiefNode.get).asScala.toList
      AgentData(agentName, environment.currentThiefNode.get, neighbours)
    }else{
      println("Invalid agent name")
      AgentData("null", ComparableNode(-1, List.empty, List.empty, false), List.empty)
    }
  }

  def action(agentName: String, moveToNeighbourId: Int): CatchMeGameEnvironment = {
    // Implement the logic to move the agent (police or thief) to the given neighbour node
    // Update the currentPoliceNode or currentThiefNode accordingly
    // Return a new Environment with the updated node
    val environment = this

    if (agentName=="police"){

      // check if moveToNeighbourId is a neighbour of currentPoliceNode
      val neighbours = environment.regionalGraph.adjacentNodes(environment.currentPoliceNode.get).asScala.toList
      val moveToNeighbour = neighbours.find(node => node.id == moveToNeighbourId)
      if (moveToNeighbour.isDefined) {
        val winner = environment.checkGameOverWinner()
        if (winner != Winner.NoOne) {
          environment.copy(currentPoliceNode = moveToNeighbour, gameState = GameState.Finished, winner = Some(winner))
        } else {
          environment.copy(currentPoliceNode = moveToNeighbour)
        }

      } else {
        println("Invalid neighbour id provided police")
        environment
      }


    }else if(agentName=="thief"){

      // check if moveToNeighbourId is a neighbour of currentThiefNode
      val neighbours = environment.regionalGraph.adjacentNodes(environment.currentThiefNode.get).asScala.toList
      val moveToNeighbour = neighbours.find(node => node.id == moveToNeighbourId.toInt)
      if (moveToNeighbour.isDefined) {
        val winner = environment.checkGameOverWinner()
        if (winner != Winner.NoOne) {
          environment.copy(currentThiefNode = moveToNeighbour, gameState = GameState.Finished, winner = Some(winner))
        } else {
          environment.copy(currentThiefNode = moveToNeighbour)
        }
      } else {
        println("Invalid neighbour id provided thief")
        environment
      }

    }else{
      println("Invalid agent name")
      environment

    }

  }

  def init(regionalGraphPath: String, queryGraphPath: String): CatchMeGameEnvironment = {
    val loadedEnvironment = loadRegionalGraph(regionalGraphPath)
      .loadQueryGraph(queryGraphPath)
      .copy(gameState = GameState.Ongoing)

    // Assuming that the regional graph is where the game takes place
    val regionalNodes = loadedEnvironment.regionalGraph.nodes().asInstanceOf[java.util.Set[ComparableNode]]
    val allNodes = regionalNodes.asScala.toList

    // Picking random nodes for police and thief
    val random = new Random()
    val randomPoliceNode = allNodes(random.nextInt(allNodes.size))
    val randomThiefNode = allNodes(random.nextInt(allNodes.size))

    loadedEnvironment.copy(
      currentPoliceNode = Some(randomPoliceNode),
      currentThiefNode = Some(randomThiefNode)
    )
  }

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
      currentPoliceNode = Some(randomPoliceNode),
      currentThiefNode = Some(randomThiefNode)
    )
  }

  def reset(regionalGraphPath: String, queryGraphPath: String): CatchMeGameEnvironment = {
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
      currentPoliceNode = Some(randomPoliceNode),
      currentThiefNode = Some(randomThiefNode)
    )
  }

}

// Singleton object to run the game environment
object CatchMeGameRunner {

  def main(args: Array[String]): Unit = {
    // Initialize the environment with some paths (you need to replace these with actual paths or test data)
    val regionalGraphTestPath = "./input/edges.txt" // Replace with actual file path
    val queryGraphTestPath = "./input/edgesPerturbed.txt" // Replace with actual file path

    // Initializing the game environment with test paths
    val environment = CatchMeGameEnvironment().init(regionalGraphTestPath, queryGraphTestPath)

    // Print out the initial state
    println(s"Initial Police Node: ${environment.currentPoliceNode}")
    println(s"Initial Thief Node: ${environment.currentThiefNode}")
    println(s"Game State: ${environment.gameState}")
    println(s"Winner: ${environment.winner.getOrElse("No winner yet")}")

    // Perform some actions to test functionality
    // You need to replace 'moveToNeighbourId' with an actual ID based on your test data
    val moveToNeighbourId = environment.getAgentData("police").adjacentNodes.head.id
    val actionTestEnvironment = environment.action("police", moveToNeighbourId)

    // Print out the state after action
    println(s"After action Police Node: ${actionTestEnvironment.currentPoliceNode}")
    println(s"After action Thief Node: ${actionTestEnvironment.currentThiefNode}")
    println(s"After action Game State: ${actionTestEnvironment.gameState}")
    println(s"After action Winner: ${actionTestEnvironment.winner.getOrElse("No winner yet")}")

    // Reset the game environment and test paths
    val resetEnvironment = actionTestEnvironment.reset(regionalGraphTestPath, queryGraphTestPath)

    // Print out the state after reset
    println(s"After reset Police Node: ${resetEnvironment.currentPoliceNode}")
    println(s"After reset Thief Node: ${resetEnvironment.currentThiefNode}")
    println(s"After reset Game State: ${resetEnvironment.gameState}")
    println(s"After reset Winner: ${resetEnvironment.winner.getOrElse("No winner yet")}")
  }
}