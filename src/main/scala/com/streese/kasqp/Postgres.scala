package com.streese.kasqp

import com.streese.kasqp.models._
import io.getquill.util.LoadConfig
import io.getquill.{JdbcContextConfig, PostgresJdbcContext, SnakeCase}
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

import scala.util.{Success, Try}

object Postgres {

  private val dataSource = JdbcContextConfig(LoadConfig("postgres")).dataSource

  private val ctx = new PostgresJdbcContext(SnakeCase, dataSource)
  import ctx._

  private val numberWord = quote(querySchema[NumberWord]("data.number_word"))
  private val wordNumber = quote(querySchema[WordNumber]("data.word_number"))

  def migrate(): MigrateResult = Flyway.configure().locations("migrations").dataSource(dataSource).load().migrate()

  def handleNumbersByWord(k: String, v: Option[String]): Try[Unit] = {
    val numbersByWord = NumbersByWord(k, v)
    if (numbersByWord.numbers.isEmpty) deleteWord(numbersByWord.word)
    else upsertNumbersByWord(numbersByWord)
  }

  private def upsertNumbersByWord(numbersByWord: NumbersByWord): Try[Unit] = Try {
    transaction {
      deleteWordUnsafe(numbersByWord.word)
      run(liftQuery(numbersByWord.numberWords).foreach(word => numberWord.insert(word).onConflictIgnore))
    }
  }

  private def deleteWord(word: String): Try[Unit] = Try(deleteWordUnsafe(word))

  private def deleteWordUnsafe(word: String) = run(numberWord.filter(_.word == lift(word)).delete)

  def handleWordsByNumber(k: String, v: Option[String]): Try[Unit] = {
    WordsByNumber(k, v).map { x =>
      if (x.words.isEmpty) deleteNumber(x.number) else upsertWordsByNumber(x)
    }.getOrElse(Success(()))
  }

  private def upsertWordsByNumber(wordsByNumber: WordsByNumber): Try[Unit] = Try {
    transaction {
      deleteNumberUnsafe(wordsByNumber.number)
      run(liftQuery(wordsByNumber.wordNumbers).foreach(number => wordNumber.insert(number).onConflictIgnore))
    }
  }

  private def deleteNumber(number: Int): Try[Unit] = Try(deleteNumberUnsafe(number))

  private def deleteNumberUnsafe(number: Int) = run(wordNumber.filter(_.number == lift(number)).delete)

}
