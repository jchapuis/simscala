package com.jcp.simscala
import com.markatta.timeforscala.{ Duration, Instant }
import Events._

object Context {

  object SimContext {
    type ReceiveEvent = (Event, SimContext) => Event

    implicit class SimContextOps(simContext: SimContext) {
      def scheduleProcess(process: ProcessActor): Process       = ???
      def scheduleProcess(receiveEvent: ReceiveEvent): Process  = ???
      def schedule(event: Event, delay: Duration): DelayedEvent = ???

      def withTime(time: Instant): SimContext = simContext.copy(time = simContext.time.copy(now = time))
      def tail: SimContext = simContext.copy(parentStack = simContext.parentStack.tail)
    }

  }

  case class ParentProcess(process: Process, callback: Any)
  case class SimTime(now: Instant, startTime: Instant)
  case class SimContext(time: SimTime, parentStack: Seq[ParentProcess])

}
