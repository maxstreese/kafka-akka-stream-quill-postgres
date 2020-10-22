package com.streese.kasqp

import akka.actor.ActorSystem
import akka.kafka.ConsumerSettings
import org.apache.kafka.common.serialization.StringDeserializer
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import com.streese.BuildInfo

object Kafka {

  def commitableSource(system: ActorSystem, topic: String) = {
    val config = system.settings.config.getConfig("kafka")
    val settings = ConsumerSettings(config, new StringDeserializer, new StringDeserializer).withGroupId(BuildInfo.name)
    Consumer.committableSource(settings, Subscriptions.topics(topic))
  }

}
