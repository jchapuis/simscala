package com.jcp.simscala.event

import com.jcp.simscala.event.Event.CallbackMessage
import com.markatta.timeforscala.Instant

case class AllOf(events: Seq[Event],
                 time: Instant,
                 callbackProcess: Process,
                 callbackMessage: CallbackMessage)
  extends Condition {
  def name = s"allOf ${events.map(_.name).mkString(", ")}"
}
