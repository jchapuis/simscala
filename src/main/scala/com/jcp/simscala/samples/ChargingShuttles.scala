package com.jcp.simscala.samples

import akka.actor.ActorSystem
import com.jcp.simscala.context.SimContext
import com.jcp.simscala.environment.DiscreteEnvironment
import com.jcp.simscala.event.Event.EventName
import com.jcp.simscala.event.{ CompositeEvent, Condition, Event, EventFactory }
import com.jcp.simscala.process._
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
      DiscreteEnvironment(EventFactory.rootProcess(ShuttleDispatch((1 to 10).map(Shuttle).toList)))
    environment.run()
    Await.ready(system.whenTerminated, scala.concurrent.duration.Duration.Inf)
  }

  case class ShuttleDispatch(shuttles: List[Shuttle])
    extends ProcessBehavior
    with ConditionBehavior
    with CallbackBehavior
    with EventFactoryBehavior {
    def name: EventName      = "ShuttleDispatch"
    def shuttleDone(id: Int) = s"shuttle${id}Done"
    val AllShuttlesDone      = "shuttlesDone"

    def start(implicit SC: SimContext): Event = {
      val subProcesses = shuttles.map(shuttle => process(shuttle, shuttleDone(shuttle.id)))
      CompositeEvent(subProcesses ++ Seq(allOf(subProcesses.map(_.endEventName), AllShuttlesDone)))
    }

    override def receiveConditionMatched(condition: Condition)(implicit SC: SimContext): Event = {
      logger.info("All shuttles done, exiting")
      processEnd
    }

    def receiveCallback[T](callback: Any, value: T)(implicit SC: SimContext): Event = callback match {
      case shuttleDone: String => {
        logger.info(shuttleDone)
        Event.Never
      }
    }
  }

  case class Shuttle(id: Int) extends ProcessBehavior with CallbackBehavior with EventFactoryBehavior {
    val ChargingDone     = "chargingDone"
    val DrivingDone      = "rideDone"
    val DrivingIncrement = 15 minutes
    val MaxDrivingTime   = 1 hour
    var totalDrivingTime = Duration.Zero

    def start(implicit simContext: SimContext) = charge(simContext)

    def receiveCallback[T](callback: Any, value: T)(implicit simContext: SimContext) =
      callback match {
        case ChargingDone =>
          logger.info("Charging done, start driving")
          drive(DrivingIncrement)
        case DrivingDone =>
          totalDrivingTime = totalDrivingTime.plus(DrivingIncrement)
          if (totalDrivingTime >= MaxDrivingTime) {
            logger.info(s"Total time driven $totalDrivingTime, heading to garage")
            processEnd
          } else {
            logger.info("Driving done, need charging")
            charge
          }
      }

    private def charge(implicit simContext: SimContext) = process(Charge(name, 5 minutes), ChargingDone)
    private def drive(duration: Duration)(implicit simContext: SimContext) =
      process(Drive(name, DrivingIncrement), DrivingDone)

    def name: EventName = s"Shuttle$id"
  }

  case class Charge(shuttleId: String, duration: Duration)
    extends ProcessBehavior
    with CallbackBehavior
    with ResourceBehavior
    with EventFactoryBehavior {
    def start(implicit SC: SimContext) = {
      logger.info("Requesting charging station")
      requestResource(ChargingStation)
    }

    def receiveCallback[T](callback: Any, value: T)(implicit SC: SimContext) = {
      logger.info("Finished charging")
      CompositeEvent(releaseResource(ChargingStation), processEnd)
    }

    def resourceAcquired[R](resource: R)(implicit SC: SimContext): Event = {
      logger.info("Start charging")
      delayedCallback(duration, None, None)
    }

    override def name: EventName = s"Charge$shuttleId"
  }

  case class Drive(shuttleId: String, duration: Duration)
    extends ProcessBehavior
    with CallbackBehavior
    with EventFactoryBehavior {
    def name: EventName = s"Drive$shuttleId"

    def start(implicit SC: SimContext): Event = {
      logger.info(s"Start driving $shuttleId until ${SC.time.now + duration}")
      delayedCallback(duration, None, None)
    }

    def receiveCallback[T](callback: Any, value: T)(implicit SC: SimContext): Event = {
      logger.info(s"Finished driving $shuttleId")
      processEnd
    }
  }

  case object ChargingStation extends FifoResource {
    override def name: String  = "ChargingStation"
    override def capacity: Int = 3
  }

}
