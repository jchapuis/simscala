package com.jcp.simscala.event

import com.jcp.simscala.event.Event.{ CallbackMessage, EventName }
import com.markatta.timeforscala.Instant

case class AnyOf(events: Seq[EventName], time: Instant, callbackProcess: Process, callbackMessage: CallbackMessage)
  extends Condition {
  def name = s"anyOf ${events.mkString(", ")}"
}
