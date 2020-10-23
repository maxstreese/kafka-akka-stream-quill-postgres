package com.streese.kasqp

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.kafka.ConsumerSettings
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.stream.SharedKillSwitch
import com.streese.BuildInfo
import org.apache.kafka.clients.admin.{Admin, NewTopic}
import org.apache.kafka.common.errors.TopicExistsException
import org.apache.kafka.common.serialization.StringDeserializer

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Try}

object Kafka {

  private val admin = Admin.create(Map[String, Object]("bootstrap.servers" -> "localhost:9092").asJava)

  def createTopics(names: String*): Unit = {
    val topics = names.map(name => new NewTopic(name, 1, 1.toShort)).asJava
    Try(admin.createTopics(topics).all.get(5, TimeUnit.SECONDS)) match {
      case Failure(e)  => if (Option(e.getCause).exists(_.isInstanceOf[TopicExistsException])) () else throw e
      case _           => ()
    }
  }

  def source(
    system: ActorSystem,
    topic: String,
    action: (String, Option[String]) => Try[Unit],
    killSwitch: SharedKillSwitch
  ) = {
    val config = system.settings.config.getConfig("kafka")
    val settings = ConsumerSettings(config, new StringDeserializer, new StringDeserializer).withGroupId(BuildInfo.name)
    Consumer.committableSource(settings, Subscriptions.topics(topic))
      .map(msg => (msg.record.key, Option(msg.record.value), msg.committableOffset))
      .map { case (k, v, c) => action(k, v) -> c }
      .wireTap { case (res, _) => res.failed.map(e => killSwitch.abort(e)) }
      .filter { case (res, _) => res.isSuccess }
      .map{ case (_, c) => c }
  }

}
