package com.jcp.simscala

import akka.actor.ActorRef
import com.jcp.simscala.Context._
import com.markatta.timeforscala.{Duration, Instant}
import monocle.Lens
import scalaz.Order

object Events {
  type EventName = String
  val endEventName     = "_end"
  val timeoutEventName = "_timeout"

  type CallbackMessage = Any

  sealed trait Event {
    def name: EventName
    def simContext: SimContext
  }

  sealed trait HasValue[T] {
    def value: T
  }

  object Event {
    implicit class EventOps[E <: Event](event: E)(implicit eventContextLens: Lens[E, SimContext]) {
      private val eventTimeLens    = eventContextLens composeLens SimContext.contextTimeLens
      def withTime(simTime: Instant): E =
        eventTimeLens.set(SimTime(eventTimeLens.get(event).initialTime, simTime))(event)
    }
  }

  case class TimeoutEvent[T](delay: Duration,
                             time: Instant,
                             simContext: SimContext,
                             callbackMessage: CallbackMessage,
                             value: T)
    extends Event
    with HasValue[T] {
    def name = simContext.processStack.head.name + timeoutEventName
  }
  object TimeoutEvent {
    implicit val ordering: Ordering[TimeoutEvent[_]] = (x: TimeoutEvent[_], y: TimeoutEvent[_]) =>
      x.time.compareTo(y.time)
    implicit val order: Order[TimeoutEvent[_]] = Order.fromScalaOrdering
  }

  case class ProcessStart(processActor: ActorRef,
                          name: EventName,
                          simContext: SimContext,
                          callbackParentOnEnd: CallbackMessage)
    extends Event

  case class ProcessEnd[T](process: ProcessStart, simContext: SimContext, value: T) extends Event with HasValue[T] {
    def name = process.name + endEventName
  }

}
