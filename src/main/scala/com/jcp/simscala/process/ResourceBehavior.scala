package com.jcp.simscala.process

import com.jcp.simscala.context.SimContext
import com.jcp.simscala.event.Event

trait ResourceBehavior {
  def resourceAcquired[R](resource: R)(implicit SC: SimContext): Event
}
