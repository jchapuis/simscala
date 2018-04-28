package com.jcp.simscala

import akka.actor.ActorSystem
import com.jcp.simscala.Events.EventName
import com.markatta.timeforscala.TimeExpressions._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn

object SmallExample {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("simscala")

      val environment =
        DiscreteEnvironment(EventFactory.rootProcess(new SampleProcess))
      environment.run()
      Await.ready(system.whenTerminated, Duration.Inf)
  }

  class SampleProcess extends ProcessBehavior {
    val callbackMessage = "callback"

    def receiveStart(simContext: Context.SimContext, eventFactory: EventFactory): Events.Event = {
      logger.info("started")
      eventFactory.timeout(10 seconds, callbackMessage, None)
    }

    def receiveContinue[T](callback: Any,
                           value: T,
                           simContext: Context.SimContext,
                           eventFactory: EventFactory): Events.Event = {
      logger.info("woken after timeout")
      eventFactory.processEnd(None)
    }

    def receiveInterrupt(cause: String, simContext: Context.SimContext, eventFactory: EventFactory): Events.Event = ???

    def name: EventName = "Sample"
  }

}
