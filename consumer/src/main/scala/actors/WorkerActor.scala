package actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import consumer.DataConsumer.{executionContext, mat, system}
import model.Formatters._
import model._

import scala.concurrent.Future

class WorkerActor extends Actor with ActorLogging{

  val cloudSender: ActorRef = context.actorOf(props = Props(classOf[CloudSender]))

  def receive: Receive = {

    case receivedHistory @ HistoryFetcher(date,pageNum,link,pageList) => {

      if(!pageList.contains(pageNum)) {
        log.info(s"$pageNum is going to be processed.")

        sendHttpRequestWithPage(link,date,pageNum.toString)
          .flatMap(httpRes => Unmarshal(httpRes.entity).to[PageResponse])
          .map(myVal => cloudSender ! myVal)
          .recover {
            case error: Throwable => {
              log.error(s"There's an error while sending the request: $pageNum")
              Thread.sleep(25)
              self ! receivedHistory
            }
          }
      }
    }

    case CloudSenderFinished(pageNumber) =>{
        context.actorSelection("akka://default/user/masterActor") ! WorkDoneResponse(pageNumber)
        log.info(s"worker $pageNumber has stopped.")
        self ! PoisonPill
    }

    case messages @ MessageList(theMessage) => {
      log.info("0 is going to be processed.")
      cloudSender ! messages
    }
  }

  def sendHttpRequestWithPage(link: String,date: String,pageNum: String): Future[HttpResponse] ={
    Http().singleRequest(HttpRequest(uri = link + date + "&page=" + pageNum))
  }
}
