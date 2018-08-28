package com.jcp.simscala.event
import com.jcp.simscala.event.Event.EventName
import com.markatta.timeforscala.Instant

case class CompositeEvent(events: List[Event]) extends Event {
  require(events.nonEmpty)
  require(events.forall(_.time == events.head.time))
  def name: EventName = s"Composite(${events.map(_.name).mkString(",")})"
  def time: Instant   = events.head.time
}

object CompositeEvent {
  def apply(firstEvent: Event, others: Event*): CompositeEvent = CompositeEvent(firstEvent :: others.toList)
}
