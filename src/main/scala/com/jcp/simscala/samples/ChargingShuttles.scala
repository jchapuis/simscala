package com.jcp.simscala.samples

import akka.actor.ActorSystem
import com.jcp.simscala.context.SimContext
import com.jcp.simscala.environment.DiscreteEnvironment
import com.jcp.simscala.event.Event.EventName
import com.jcp.simscala.event.{ CompositeEvent, Event, EventFactory }
import com.jcp.simscala.process.{ CallbackBehavior, ProcessBehavior, ResourceBehavior }
import com.jcp.simscala.resource.FifoResource
import com.markatta.timeforscala.Duration
import com.markatta.timeforscala.TimeExpressions._
import com.markatta.timeforscala._

import scala.concurrent.Await
import scala.language.postfixOps

object ChargingShuttles {
  implicit val system = ActorSystem("ChargingShuttles")

  def main(args: Array[String]): Unit = {
    val environment =
      DiscreteEnvironment(EventFactory.initialProcess(ShuttleDispatch((1 to 10).map(Shuttle).toList)))
    environment.run()
    Await.ready(system.whenTerminated, scala.concurrent.duration.Duration.Inf)
  }

  case class ShuttleDispatch(shuttles: List[Shuttle]) extends ProcessBehavior with CallbackBehavior {
    def name: EventName      = "ShuttleDispatch"
    def shuttleDone(id: Int) = s"shuttle${id}Done"
    val AllShuttlesDone      = "shuttlesDone"
    def receiveStart(simContext: SimContext): Event =
      simContext.eventFactory.allOf(
        shuttles.map(shuttle => simContext.eventFactory.process(shuttle, shuttleDone(shuttle.id))),
        AllShuttlesDone
      )

    override def receiveCallback[T](callback: Any, value: T, simContext: SimContext): Event = callback match {
      case AllShuttlesDone => {
        logger.info("All shuttles done, exiting")
        simContext.eventFactory.processEnd(None)
      }
      case shuttleDone: String => {
        logger.info(shuttleDone)
        Event.Never
      }
    }
  }

  case class Shuttle(id: Int) extends ProcessBehavior with CallbackBehavior {
    val ChargingDone     = "chargingDone"
    val DrivingDone      = "rideDone"
    val DrivingIncrement = 15 minutes
    val MaxDrivingTime   = 1 hour
    var totalDrivingTime = Duration.Zero

    def receiveStart(simContext: SimContext) = charge(simContext)

    def receiveCallback[T](callback: Any, value: T, simContext: SimContext) =
      callback match {
        case ChargingDone =>
          logger.info("Charging done, start driving")
          drive(DrivingIncrement, simContext)
        case DrivingDone =>
          totalDrivingTime = totalDrivingTime.plus(DrivingIncrement)
          if (totalDrivingTime >= MaxDrivingTime) {
            logger.info("Total distance driven, heading to garage")
            simContext.eventFactory.processEnd(None)
          } else {
            logger.info("Driving done, need charging")
            charge(simContext)
          }
      }

    def charge(simContext: SimContext) = simContext.eventFactory.process(Charge(name, 5 minutes), ChargingDone)
    def drive(duration: Duration, simContext: SimContext) =
      simContext.eventFactory.process(Drive(DrivingIncrement), DrivingDone)

    def name: EventName = s"Shuttle$id"
  }

  case class Charge(shuttleId: String, duration: Duration)
    extends ProcessBehavior
    with CallbackBehavior
    with ResourceBehavior {
    def receiveStart(simContext: SimContext) = {
      logger.info("Requesting charging station")
      simContext.eventFactory.requestResource(ChargingStation)
    }

    def receiveCallback[T](callback: Any, value: T, simContext: SimContext) = {
      logger.info("Finished charging")
      CompositeEvent(simContext.eventFactory.releaseResource(ChargingStation), simContext.eventFactory.processEnd(None))
    }

    def resourceAcquired[R](resource: R, simContext: SimContext): Event = {
      logger.info("Start charging")
      simContext.eventFactory.delayedCallback(duration, None, None)
    }

    override def name: EventName = s"Charge$shuttleId"
  }

  case class Drive(duration: Duration) extends ProcessBehavior with CallbackBehavior {
    def name: EventName = getClass.getSimpleName

    def receiveStart(simContext: SimContext): Event = {
      logger.info(s"Start driving")
      simContext.eventFactory.delayedCallback(duration, None, None)
    }

    def receiveCallback[T](callback: Any, value: T, simContext: SimContext): Event = {
      logger.info(s"Finished driving")
      simContext.eventFactory.processEnd(None)
    }
  }

  case object ChargingStation extends FifoResource {
    override def name: String  = "ChargingStation"
    override def capacity: Int = 3
  }

}
