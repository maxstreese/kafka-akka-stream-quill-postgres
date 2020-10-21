package com.streese.kasqp

object models {

  case class NumberWord(word: String, number: Int)

  case class NumbersByWord(word: String, numbers: Seq[Int]) {
    def numberWords: Seq[NumberWord] = numbers.map(n => NumberWord(word, n))
  }

  object NumbersByWord {
    def apply(k: String, v: String): Option[NumbersByWord] = ???
  }

  case class WordNumber(number: Int, word: String)

  case class WordsByNumber(number: Int, words: Seq[String]) {
    def wordNumbers: Seq[WordNumber] = words.map(w => WordNumber(number, w))
  }

  object WordsByNumber {
    def apply(k: String, v: String): Option[WordsByNumber] = ???
  }

}
