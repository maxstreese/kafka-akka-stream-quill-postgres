package com.streese.kasqp

import akka.actor.ActorSystem
import akka.kafka.CommitterSettings
import akka.kafka.scaladsl.Committer
import akka.stream.KillSwitches
import akka.stream.scaladsl.Merge
import akka.stream.scaladsl.Source
import com.streese.BuildInfo

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {

  val topicWordsByNumber = "words-by-number"
  val topicNumbersByWord = "numbers-by-word"

  Kafka.createTopics(topicWordsByNumber, topicNumbersByWord)
  Postgres.migrate()

  implicit val system = ActorSystem(BuildInfo.name)

  val killSwitch = KillSwitches.shared("kafka-commit-kill-switch")

  val wordsByNumberResults = Kafka.source(
    system, topicWordsByNumber, Postgres.handleWordsByNumber, killSwitch
  )

  val numbersByWordResults = Kafka.source(
    system, topicNumbersByWord, Postgres.handleNumbersByWord, killSwitch
  )

  val done = Source
    .combine(wordsByNumberResults, numbersByWordResults)(Merge(_))
    .via(killSwitch.flow)
    .runWith(Committer.sink(CommitterSettings(system).withMaxBatch(1)))

  done.onComplete { res =>
    println(res)
    system.terminate()
  }

}
