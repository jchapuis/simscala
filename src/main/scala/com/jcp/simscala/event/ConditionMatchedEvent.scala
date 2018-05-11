package com.jcp.simscala.event

import com.jcp.simscala.event.Event.EventName
import com.markatta.timeforscala.Instant

case class ConditionMatchedEvent(condition: Condition, time: Instant) extends Event {
  override def name: EventName = s"condition match: ${condition.name}"
}
