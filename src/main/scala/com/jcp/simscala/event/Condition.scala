package com.jcp.simscala.event

import com.jcp.simscala.event.Event.{CallbackMessage, EventName}

trait Condition extends Event {
  def events: Seq[EventName]
  def callbackProcess: Process
  def callbackMessage: CallbackMessage
}
