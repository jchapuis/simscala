package com.jcp.simscala.process

import com.jcp.simscala.context.SimContext
import com.jcp.simscala.event.{Condition, Event}

trait ConditionBehavior {
  def receiveConditionMatched(condition: Condition)(implicit SC: SimContext): Event
}
