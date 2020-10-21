package com.streese.kasqp

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Merge
import akka.kafka.scaladsl.Committer
import akka.kafka.CommitterSettings
import com.streese.BuildInfo
import com.streese.kasqp.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import akka.stream.KillSwitches

object Main extends App {

  Postgres.migrate()

  implicit val system = ActorSystem(BuildInfo.name)

  val killSwitch = KillSwitches.shared("kafka-commit-kill-switch")

  val wordsByNumberResults = Kafka.commitableSource(system, "words-by-number")
    .mapConcat { msg =>
      val words = Option(msg.record.value())
        .map(_.split(",").toSeq)
        .getOrElse(Seq.empty)
      for (n <- Try(msg.record.key().toInt).toOption.toList) yield WordsByNumber(n, words) -> msg.committableOffset
    }
    .map { case (m, c) =>
      val res = if (m.words.isEmpty) Postgres.deleteNumber(m.number) else Postgres.upsertWordNumbers(m.wordNumbers)
      res -> c
    }

  val numbersByWordResults = Kafka.commitableSource(system, "numbers-by-word")
    .map { msg =>
      val numbers = Option(msg.record.value())
        .flatMap(s => Try(s.split(",").toSeq.map(_.toInt)).toOption)
        .getOrElse(Seq.empty)
      NumbersByWord(msg.record.key(), numbers) -> msg.committableOffset
    }
    .map { case (m, c) =>
      val res = if (m.numbers.isEmpty) Postgres.deleteWord(m.word) else Postgres.upsertNumberWords(m.numberWords)
      res -> c
    }

    val done = Source
      .combine(wordsByNumberResults, numbersByWordResults)(Merge(_))
      .via(killSwitch.flow)
      .wireTap { case (res, _) => if (res.isFailure) killSwitch.shutdown() }
      .filter { case (res, _) => res.isSuccess }
      .map { case (_, c) => c}
      .runWith(Committer.sink(CommitterSettings(system).withMaxBatch(1)))

    done.onComplete { msg =>
      println(msg)
      system.terminate()
    }

}
