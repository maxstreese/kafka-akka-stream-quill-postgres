package com.streese.kasqp

import com.streese.kasqp.models._
import io.getquill.util.LoadConfig
import io.getquill.{JdbcContextConfig, PostgresJdbcContext, SnakeCase}
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

import scala.util.Try

object Postgres {

  private val dataSource = JdbcContextConfig(LoadConfig("postgres")).dataSource

  private val ctx = new PostgresJdbcContext(SnakeCase, dataSource)
  import ctx._

  private val numberWord = quote(querySchema[NumberWord]("data.number_word"))
  private val wordNumber = quote(querySchema[WordNumber]("data.word_number"))

  def migrate(): MigrateResult = Flyway.configure().locations("migrations").dataSource(dataSource).load().migrate()

  def upsertNumbersByWord(numbersByWord: NumbersByWord): Try[Unit] = Try {
    transaction {
      deleteWordUnsafe(numbersByWord.word)
      run(liftQuery(numbersByWord.numberWords).foreach(word => numberWord.insert(word).onConflictIgnore))
    }
  }

  def deleteWord(word: String): Try[Unit] = Try(deleteWordUnsafe(word))

  private def deleteWordUnsafe(word: String) = run(numberWord.filter(_.word == lift(word)).delete)

  def upsertWordsByNumber(wordsByNumber: WordsByNumber): Try[Unit] = Try {
    transaction {
      deleteNumberUnsafe(wordsByNumber.number)
      run(liftQuery(wordsByNumber.wordNumbers).foreach(number => wordNumber.insert(number).onConflictIgnore))
    }
  }

  def deleteNumber(number: Int): Try[Unit] = Try(deleteNumberUnsafe(number))

  private def deleteNumberUnsafe(number: Int) = run(wordNumber.filter(_.number == lift(number)).delete)

}
