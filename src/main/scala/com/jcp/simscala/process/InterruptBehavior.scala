package com.jcp.simscala.process

import com.jcp.simscala.context.SimContext
import com.jcp.simscala.event.Event

trait InterruptBehavior {
  def interrupted(cause: String)(implicit SC: SimContext): Event
}
