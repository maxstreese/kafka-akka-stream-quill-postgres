package com.streese.kasqp

import scala.util.Try

object models {

  case class WordNumber(number: Int, word: String)

  case class WordsByNumber(number: Int, words: Seq[String]) {
    def wordNumbers: Seq[WordNumber] = words.map(w => WordNumber(number, w))
  }

  object WordsByNumber {
    def apply(k: String, v: Option[String]): Option[WordsByNumber] = {
      val words = v
      .map(_.stripLineEnd)
      .map(_.split(",").toSeq)
      .getOrElse(Seq.empty)
      for (n <- Try(k.toInt).toOption) yield WordsByNumber(n, words)
    }
  }

  case class NumberWord(word: String, number: Int)

  case class NumbersByWord(word: String, numbers: Seq[Int]) {
    def numberWords: Seq[NumberWord] = numbers.map(n => NumberWord(word, n))
  }

  object NumbersByWord {
    def apply(k: String, v: Option[String]): NumbersByWord = {
      val numbers = v
        .map(_.stripLineEnd)
        .flatMap(s => Try(s.split(",").toSeq.map(_.toInt)).toOption)
        .getOrElse(Seq.empty)
      NumbersByWord(k, numbers)
    }
  }

}
