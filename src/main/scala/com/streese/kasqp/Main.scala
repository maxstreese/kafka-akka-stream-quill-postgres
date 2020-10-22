package com.streese.kasqp

import akka.actor.ActorSystem
import akka.kafka.CommitterSettings
import akka.kafka.scaladsl.Committer
import akka.stream.KillSwitches
import akka.stream.scaladsl.Merge
import akka.stream.scaladsl.Source
import com.streese.BuildInfo
import com.streese.kasqp.models._

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {

  val topicWordsByNumber = "words-by-number"
  val topicNumbersByWord = "numbers-by-word"

  Kafka.createTopics(topicWordsByNumber, topicNumbersByWord)
  Postgres.migrate()

  implicit val system = ActorSystem(BuildInfo.name)

  val killSwitch = KillSwitches.shared("kafka-commit-kill-switch")

  val wordsByNumberResults = Kafka.commitableSource(system, topicWordsByNumber)
    .mapConcat(msg => WordsByNumber(msg.record.key, msg.record.value).toList.map(_ -> msg.committableOffset))
    .map { case (m, c) =>
      val res = if (m.words.isEmpty) Postgres.deleteNumber(m.number) else Postgres.upsertWordsByNumber(m)
      res -> c
    }

  val numbersByWordResults = Kafka.commitableSource(system, topicNumbersByWord)
    .map(msg => NumbersByWord(msg.record.key, msg.record.value) -> msg.committableOffset)
    .map { case (m, c) =>
      val res = if (m.numbers.isEmpty) Postgres.deleteWord(m.word) else Postgres.upsertNumbersByWord(m)
      res -> c
    }

  val done = Source
    .combine(wordsByNumberResults, numbersByWordResults)(Merge(_))
    .via(killSwitch.flow)
    .wireTap { case (res, _) =>
      if (res.isFailure) {
        println(res)
        killSwitch.shutdown()
      }
    }
    .filter { case (res, _) => res.isSuccess }
    .map { case (_, c) => c }
    .runWith(Committer.sink(CommitterSettings(system).withMaxBatch(1)))

  done.onComplete { res =>
    println(res)
    system.terminate()
  }

}
