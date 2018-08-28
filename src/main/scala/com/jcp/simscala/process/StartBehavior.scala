package com.jcp.simscala.process

import com.jcp.simscala.context.SimContext
import com.jcp.simscala.event.Event

trait StartBehavior {
  def start(implicit SC: SimContext): Event
}
