package com.jcp.simscala

import java.time.Instant

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import com.markatta.timeforscala._

import scala.collection.mutable
import scala.io.StdIn
import Events._
import Commands._
import Context._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps

object SimMaster {
  def props(): Props = Props(new SimMaster)

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("simscala")
    try {
      val environment = system.actorOf(SimMaster.props(), "simMaster")
      // Exit the system after ENTER is pressed
      StdIn.readLine()
    } finally {
      system.terminate()
    }
  }
}

class SimMaster extends Actor with ActorLogging {
  implicit val timeout: Timeout = Timeout(5 seconds) // needed for `?` below
  import scala.concurrent.ExecutionContext.Implicits.global

  sealed trait SimStep
  case object JumpStep                                                    extends SimStep
  case class ProcessStep(process: Process, parentStack: Seq[ProcessStep]) extends SimStep
  private case class TimedEvent(time: Instant, event: Event)
  private object TimedEvent {
    implicit val ordering: Ordering[TimedEvent] = (x: TimedEvent, y: TimedEvent) => x.time.compareTo(y.time)
  }
  private val eventQueue = mutable.PriorityQueue[TimedEvent]()
  private val start      = Instant.now

  override def receive: Receive = {
    case DelayedEvent(event, delay, simContext) =>
      eventQueue += TimedEvent(simContext.time.now + delay, event)
      val TimedEvent(nextInstant, nextEvent) = eventQueue.dequeue()
      self ! nextEvent.withTime(nextInstant)
    case Process(actorRef, simContext) =>
      actorRef ? StartCommand(simContext) pipeTo self
    case EndEvent(_, simContext) =>
      val ParentProcess(Process(parentActor, _), callback) = simContext.parentStack.head
      parentActor ? ContinueCommand(callback, simContext.tail) pipeTo self
  }

}
