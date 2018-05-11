package com.jcp.simscala.process

import com.jcp.simscala.context.SimContext
import com.jcp.simscala.event.Event

trait InterruptBehavior {
  def receiveInterrupt(cause: String, simContext: SimContext): Event
}
