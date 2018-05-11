package com.jcp.simscala.environment

import java.time.Instant

import com.jcp.simscala.event.Event.EventName

import scala.language.postfixOps

sealed trait StopCondition

case object StopCondition {
  case object NoMoreEvents extends StopCondition
  case class SomeEventTriggered(eventName: EventName) extends StopCondition
  case class SomeInstantReached(instant: Instant) extends StopCondition
}









