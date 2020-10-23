# General

An app which consumes two unrelated topics from Kafka via Akka Streams and persists both to a Postgres database using Quill.

The reason I wrote this app is that I wanted to figure out how I can achieve the below bullet-points in a way that feels easy and elegant to me:

- Commit the offset of any given record if and only if the record was successfully handled by the database
- Handle more than one topic while ensuring that the application crashes as soon as any part of any topics stream fails
- Handle records that require more than one database statement to be executed (i.e. use of transactions)

# Requirements

In order to play with this repository yourself you need:

- JDK version 8 or above
- SBT (any version will do as SBT pulls the SBT version defined in the project automatically)
- Docker (not sure about the version) and Docker Compose version 1.27.4 or above

# Possible Improvements

- Use [Testcontainers](https://www.testcontainers.org/) instead of Docker Compose
- Implement integration tests for showcasing (as well as proofing) that the application fails fast
