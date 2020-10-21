CREATE SCHEMA data;

CREATE TABLE data.number_word(
  "word"   TEXT    NOT NULL,
  "number" INTEGER NOT NULL,
  PRIMARY KEY ("word", "number")
);

CREATE TABLE data.word_number(
  "number" INTEGER NOT NULL,
  "word"   TEXT    NOT NULL,
  PRIMARY KEY ("number", "word")
);
