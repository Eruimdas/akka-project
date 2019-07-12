package producer

import com.typesafe.config.{Config, ConfigFactory}

trait ProducerConfig {
  val myConfig: Config = ConfigFactory.load()
  val myPort : String = myConfig.getString("producer.port")
  val myHost : String = myConfig.getString("producer.host")
}