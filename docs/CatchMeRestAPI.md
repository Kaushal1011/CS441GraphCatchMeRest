# Catch Me Rest API

Postman Collection Documentation: [https://documenter.getpostman.com/view/30303534/2s9YeD8YTz](https://documenter.getpostman.com/view/30303534/2s9YeD8YTz)

## Getting Started

### Prerequisites

- Scala
- SBT (Scala Build Tool)
- Docker and Docker Compose

### Running Locally

1. Clone the repository:
   ```bash
   git clone https://github.com/Kaushal1011/CS441GraphCatchMeRest.git
   
   ```
2. Navigate to the project directory:
   ```bash
   cd CS441GraphCatchMeRest
   ```
3. Compile and run the application:
   ```bash
   sbt clean compile run
   ```

This will start the application on port 8080.

### Using Docker and Docker Compose

1. Build the Docker image:
   ```bash
   docker compose up --build
   ```
2. Start the application using Docker Compose:
   ```bash
   docker compose up
   ```
   This will start both the application on port 80 and Nginx as a reverse proxy.

## API Routes

1. **Action (`/action`)**: POST request to perform an action (move police or thief). This endpoint is used by the players to move their respective agent.
2. **Initialize Game (`/init`)**: POST request to initialize the game environment. Initializes the game with the given graph files. Requires Auth.
3. **Reset Game (`/reset`)**: POST request to reset the game environment. Requires Auth. This endpoint is used to reset the game environment.
4. **Query Graph (`/querygraph`)**: GET request to retrieve the current state of the game graph. This endpoint returns the current url of the graph file. which can be used by agent.
5. **Query Agent (`/queryagent`)**: POST request to get data about a specific agent (police or thief). This endpoint returns the current position of the agents and the winner if any.


## Project Structure

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

### Protobufs

* `winner.proto` -- contains the protobuf for the winner request and response

### Misc 

* `docker-compose.yml` -- Docker Compose configuration file
* `nginx.conf` -- Nginx configuration file
* `build.sbt` -- SBT build file
* `Dockerfile` -- Dockerfile for the application

The rest api uses a special flag in config `invalid-makes-loss = "false"` the enables disables the losing on invalid move. 