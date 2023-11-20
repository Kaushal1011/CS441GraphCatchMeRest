# Use an official Scala runtime as a parent image
FROM hseeberger/scala-sbt:eclipse-temurin-17.0.2_1.6.2_2.13.8


# Set the working directory in the container
WORKDIR /usr/src/app

# Define environment variable
ENV JAVA_OPTS=""

# The CMD command can be overridden by docker-compose.yml
CMD ["sbt", "run"]