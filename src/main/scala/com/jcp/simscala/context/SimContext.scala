package com.jcp.simscala.context

import akka.actor.ActorSystem
import com.jcp.simscala.event.{Condition, Event, EventFactory, Process}
import com.markatta.timeforscala.Instant
import monocle.macros.GenLens

case class SimContext(time: SimTime,
                      processStack: Seq[Process],
                      conditions: Seq[Condition],
                      triggeredEvents: Set[Event])

object SimContext {
  val init                             = SimContext(SimTime.epoch, Nil, Nil, Set())
  val contextLens: GenLens[SimContext] = GenLens[SimContext]
  val contextTimeLens                  = contextLens(_.time)
  val processStackLens                 = contextLens(_.processStack)
  val conditionsLens                   = contextLens(_.conditions)
  val triggeredEventsLens              = contextLens(_.triggeredEvents)

  implicit class SimContextOps(simContext: SimContext)(implicit AS: ActorSystem) {
    def stackHead: Process                              = simContext.processStack.head
    def withStackTail: SimContext                       = processStackLens.modify(_.tail)(simContext)
    def pushOnStack(process: Process): SimContext       = processStackLens.modify(Seq(process) ++ _)(simContext)
    def withTime(time: Instant): SimContext             = contextTimeLens.modify(t => SimTime(time, t.initialTime))(simContext)
    def now: String                                     = contextTimeLens.get(simContext).now.toString
    def withCondition(condition: Condition): SimContext = conditionsLens.modify(_ :+ condition)(simContext)
    def matchingConditions: Seq[Condition] =
      simContext.conditions.filter(c => c.events.forall(simContext.triggeredEvents.contains))
    def withoutCondition(condition: Condition): SimContext =
      conditionsLens.modify(_.filterNot(_ == condition))(simContext)
    def withTriggeredEvent(event: Event): SimContext = triggeredEventsLens.modify(_ + event)(simContext)
    def eventFactory: EventFactory                   = EventFactory(simContext)
  }
}
