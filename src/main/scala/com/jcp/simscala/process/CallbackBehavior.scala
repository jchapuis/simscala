package com.jcp.simscala.process

import com.jcp.simscala.context.SimContext
import com.jcp.simscala.event.Event

trait CallbackBehavior {
  def receiveCallback[T](callback: Any, value: T)(implicit SC: SimContext): Event
}
