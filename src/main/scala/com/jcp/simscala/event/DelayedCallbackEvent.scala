package com.jcp.simscala.event

import com.jcp.simscala.event.Event.CallbackMessage
import com.markatta.timeforscala.{ Duration, Instant }
import com.markatta.timeforscala._

case class DelayedCallbackEvent[T](delay: Duration,
                                   creationTime: Instant,
                                   callbackProcess: Process,
                                   callbackMessage: CallbackMessage,
                                   value: T)
  extends Event
  with HasValue[T] {
  def name                   = s"delayed: ${callbackProcess.name}"
  override def time: Instant = creationTime + delay
}
