package consumer

import com.typesafe.config.{Config, ConfigFactory}

trait consumerConfig {
  val consumerConfig : Config = ConfigFactory.load()
  val myDate : String = consumerConfig.getString("consumer.date")
  val myLink : String = consumerConfig.getString("consumer.link")
}