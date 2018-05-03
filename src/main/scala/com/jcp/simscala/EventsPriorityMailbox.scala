package com.jcp.simscala

import akka.dispatch.{Envelope, UnboundedStablePriorityMailbox}
import com.markatta.timeforscala.Instant

case class TimedMessage(time: Instant, message: Any)

class TimedMessagesPriorityMailbox extends UnboundedStablePriorityMailbox(
  (o1: Envelope, o2: Envelope) => (o1.message, o2.message) match {
    case (TimedMessage(t1, _), TimedMessage(t2, _)) => t1.compareTo(t2)
    case (_, _) => 0  // rely on FIFO
  }
)
