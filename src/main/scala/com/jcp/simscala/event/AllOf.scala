package com.jcp.simscala.event

import com.jcp.simscala.event.Event.{ CallbackMessage, EventName }
import com.markatta.timeforscala.Instant

case class AllOf(events: Seq[EventName], time: Instant, callbackProcess: Process, callbackMessage: CallbackMessage)
  extends Condition {
  def name = s"allOf ${events.mkString(", ")}"
}
