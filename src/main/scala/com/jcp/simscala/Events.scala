package com.jcp.simscala

import akka.actor.ActorRef
import com.markatta.timeforscala.{Duration, Instant, _}

object Events {
  type EventName = String
  val endEventName    = "_end"
  val delayedCallback = "_delayedcallback"
  val allOf           = "allOf"
  val anyOf           = "anyOf"

  type CallbackMessage = Any

  sealed trait Event {
    def name: EventName
    def time: Instant
  }

  sealed trait HasValue[T] {
    def value: T
  }

  case class DelayedCallbackEvent[T](delay: Duration,
                                     creationTime: Instant,
                                     callbackProcess: Process,
                                     callbackMessage: CallbackMessage,
                                     value: T)
    extends Event
    with HasValue[T] {
    def name                   = callbackProcess.name + delayedCallback
    override def time: Instant = creationTime + delay
  }

  case class Process(processActor: ActorRef,
                     name: EventName,
                     time: Instant,
                     callbackMessageOnEnd: CallbackMessage)
    extends Event {}

  case class ProcessEnd[T](process: Process, time: Instant, value: T)
    extends Event
    with HasValue[T] {
    def name = process.name + endEventName
  }

  case class ConditionMatchedEvent(condition: Condition, time: Instant) extends Event {
    override def name: EventName = s"condition match: ${condition.name}"
  }

  trait Condition extends Event {
    def events: Seq[Event]
    def callbackProcess: Process
    def callbackMessage: CallbackMessage
  }

  case class AllOf(events: Seq[Event],
                   time: Instant,
                   callbackProcess: Process,
                   callbackMessage: CallbackMessage)
    extends Condition {
    def name = s"$allOf ${events.map(_.name).mkString(", ")}"
  }

  case class AnyOf(events: Seq[Event],
                   time: Instant,
                   callbackProcess: Process,
                   callbackMessage: CallbackMessage)
    extends Condition {
    def name = s"$anyOf ${events.map(_.name).mkString(", ")}"
  }
}
