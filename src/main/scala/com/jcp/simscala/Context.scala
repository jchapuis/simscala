package com.jcp.simscala
import com.jcp.simscala.Events._
import com.markatta.timeforscala.Instant
import monocle.macros.GenLens
import scalaz.Heap

object Context {
  case class SimTime(now: Instant, initialTime: Instant)
  object SimTime {
    val epoch = SimTime(Instant.Epoch, Instant.Epoch)
  }
  case class SimContext(time: SimTime, processStack: Seq[ProcessStart], eventQueue: Heap[TimeoutEvent[_]])

  object SimContext {
    val init = SimContext(SimTime.epoch, Nil, Heap.Empty[TimeoutEvent[_]])
    val contextLens: GenLens[SimContext] = GenLens[SimContext]
    val contextTimeLens                  = contextLens(_.time)
    val eventQueueLens                   = contextLens(_.eventQueue)
    val processStackLens = contextLens(_.processStack)

    implicit class SimContextOps(simContext: SimContext) {
      def stackHead: ProcessStart = simContext.processStack.head
      def withStackTail: SimContext = processStackLens.modify(_.tail)(simContext)
      def pushOnStack(process: ProcessStart): SimContext = processStackLens.modify(Seq(process) ++ _)(simContext)
      def enqueueTimeout(timeout: TimeoutEvent[_]): SimContext =
        eventQueueLens.modify(_ + timeout)(simContext)
      def dequeueNextTimeout(): (TimeoutEvent[_], SimContext) = {
        val nextTimeout = eventQueueLens.get(simContext).minimum
        val newContext = eventQueueLens.modify(_.deleteMin)(simContext).withTime(nextTimeout.time)
        (nextTimeout, newContext)
      }
      def withTime(time: Instant): SimContext = contextTimeLens.modify(t => SimTime(time, t.initialTime))(simContext)
      def now: String = contextTimeLens.get(simContext).now.toString
    }
  }
}


