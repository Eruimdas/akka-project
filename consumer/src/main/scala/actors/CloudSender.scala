package actors

import java.util.Properties

import akka.actor.{Actor, ActorLogging, PoisonPill, ReceiveTimeout}
import model.{Message, MessageList, PageResponse}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.duration._

class CloudSender extends Actor with ActorLogging with CloudSenderTrait {

  val producer: KafkaProducer[String, String] = createKafkaProducer()
  context.setReceiveTimeout(50000 milliseconds)

  override def receive: Receive = {

    case  PageResponse(date,pageNumber,receivedPageResponse) => {

      log.debug("Message has been received cloud: " + pageNumber)

      receivedPageResponse.foreach { message =>
        sendToCloud(producer,topic,message)
      }


      log.info("Cloud sender has sent the values to the cloud. " + pageNumber)
      log.debug("The sender has finished.")

      producer.close()
      sender ! Message("done")
      self ! PoisonPill
    }

    case ReceiveTimeout => {
      log.info("The Timeout has been completed. The Actor will be killed now.")
      producer.close()
      context.parent ! PoisonPill
      self ! PoisonPill
    }

    case MessageList(receivedMessage) => {
      receivedMessage.foreach { message =>
        sendToCloud(producer,topic,message)
      }

      log.info("Cloud sender has sent the values to the cloud. " + "0")
      log.debug("The sender has finished.")

      producer.close()
      sender ! Message("done")
      self ! PoisonPill
    }
  }

  case class KafkaProducerConfigs(brokerList: String = "localhost:9092") {
    val properties = new Properties()
    properties.put("bootstrap.servers", brokerList)
    properties.put("key.serializer", classOf[StringSerializer])
    properties.put("value.serializer", classOf[StringSerializer])
  }

  def sendToCloud(producer: KafkaProducer[String, String], topic : String, message: Message): Unit ={
      val putRecord = new ProducerRecord[String, String](topic, message.toString())
      producer.send(putRecord)
  }

  def createKafkaProducer(): KafkaProducer[String , String] ={
    new KafkaProducer[String, String](KafkaProducerConfigs().properties)
  }
  }
