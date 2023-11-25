# Client Documentation for Policeman and Thief Graph Game

## Introduction

The Policeman and Thief Graph Game Client is a sophisticated component of the overall game project, 
designed to interact with the GraphCatchMeGameRestAPI. 
This client autonomously plays the game by making strategic decisions based on various algorithms. 
The client's architecture allows for a range of strategies to be employed, each with its unique approach to navigating the game environment.

## How to Run

## Setup (once you're in the client directory)

```bash

sbt clean compile

sbt test // to run tests
```



### SBT

To run the client, you need to execute the main class with specific parameters. The usage is as follows (if running from sbt):

```
sbt run <apiUrl> <policeStrategyName> <thiefStrategyName> <turnsForEachPlayer> <simulationIters> [regionalGraphPath] [queryGraphPath]
```

### IntelliJ

To run the client, you need to execute the main class with specific parameters. The usage is as follows (if running from intellij):

```
Main <apiUrl> <policeStrategyName> <thiefStrategyName> <turnsForEachPlayer> <simulationIters> [regionalGraphPath] [queryGraphPath]
```

- `apiUrl`: The URL of the REST API server.
- `policeStrategyName`: The strategy to be used by the police agent.
- `thiefStrategyName`: The strategy to be used by the thief agent.
- `turnsForEachPlayer`: The maximum number of turns for each player.
- `simulationIters`: The number of times the simulation will run.
- `regionalGraphPath` (optional): Path to the regional graph.
- `queryGraphPath` (optional): Path to the query graph.




## Strategy Explanation

The client employs a variety of strategies to play the game. Each strategy has a different approach to decision-making.

### Base Strategies

- **BaseStrategy**: Makes random moves from the available adjacent nodes.
- **ChaseEnemyNodeStrategy**: Moves towards the closest node to the enemy agent.
- **ChaseValuableDataStrategy**: Targets the closest node with valuable data.

### Probabilistic Strategies

- **ProbabilisticChaseEnemyNodeStrategy**: Similar to `ChaseEnemyNodeStrategy`, but with a probability of random exploration.
- **ProbabilisticChaseValuableDataStrategy**: Similar to `ChaseValuableDataStrategy`, but with a probability of random exploration.

### Confidence-Based Strategies

- **ConfidenceBaseStrategy**: Focuses on high-confidence nodes, moving randomly among them.
- **ConfidenceChaseEnemyNodeStrategy**: Chases the enemy, but only through nodes with high confidence.
- **ConfidenceChaseValuableDataStrategy**: Moves towards valuable data, prioritizing high-confidence nodes.

### Probabilistic and Confidence-Based Strategies

- **ProbabilisticBaseStrategyConf**: Combines probabilistic exploration with a preference for high-confidence nodes.
- **ProbabilisticChaseEnemyNodeStrategyConf**: Chases the enemy with probabilistic exploration, focusing on high-confidence nodes.
- **ProbabilisticChaseValuableDataStrategyConf**: Targets valuable data with probabilistic exploration, favoring high-confidence nodes.

## Class Architecture

- **Agent**: Represents a player in the game, capable of employing different strategies.
- **ComparableNode**: Represents a node in the game graph.
- **GameState**: Keeps track of the current state of the game, including player positions and the winner.
- **AgentData**: Stores data related to an agent, such as its current location and adjacent nodes.
- **Strategies**: Different strategy classes implement various approaches to playing the game.
- **Utilities**: Includes helper functions for graph operations and interactions with the REST API.

Each strategy class extends from `BaseStrategy` and overrides the `decideMove` method to implement its specific strategy. The client uses the REST API to interact with the game server, making decisions based on the current game state and the chosen strategy.

