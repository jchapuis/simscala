package com.jcp.simscala

import akka.actor.ActorSystem
import com.jcp.simscala.Context.SimContext
import com.jcp.simscala.Events.EventName
import com.markatta.timeforscala.Duration
import com.markatta.timeforscala.TimeExpressions._

import scala.concurrent.Await

object SmallExample {
  implicit val system = ActorSystem("simscala")

  def main(args: Array[String]): Unit = {

    val environment =
      DiscreteEnvironment(EventFactory.initialProcess(new Shuttle))
    environment.run()
    Await.ready(system.whenTerminated, scala.concurrent.duration.Duration.Inf)
  }

  class Shuttle extends ProcessBehavior with CallbackBehavior {
    val ChargingDone = "chargingDone"
    val RideDone     = "rideDone"

    def receiveStart(simContext: Context.SimContext) = charge(simContext)

    override def receiveCallback[T](callback: Any, value: T, simContext: Context.SimContext) =
      callback match {
        case ChargingDone =>
          logger.info(s"Charging done, start driving at ${simContext.time}")
          simContext.eventFactory.delayedCallback(10 minutes, RideDone)
        case RideDone =>
          logger.info(s"Ride done, heading to parking at ${simContext.time}")
          charge(simContext)
      }

    def charge(simContext: SimContext) = simContext.eventFactory.process(new Charge(5 minutes), ChargingDone)

    def name: EventName = "Sample"
  }

  class Charge(duration: Duration) extends ProcessBehavior with CallbackBehavior {
    def receiveStart(simContext: Context.SimContext) = {
      logger.info(s"Start parking and charging at ${simContext.time}, for $duration")
      simContext.eventFactory.delayedCallback(duration, None, None)
    }

    override def receiveCallback[T](callback: Any, value: T, simContext: Context.SimContext) =
      simContext.eventFactory.processEnd(None)

    override def name: EventName = "Charge battery"
  }

}
