package com.jcp.simscala.event

import com.jcp.simscala.event.Event.CallbackMessage

trait Condition extends Event {
  def events: Seq[Event]
  def callbackProcess: Process
  def callbackMessage: CallbackMessage
}
