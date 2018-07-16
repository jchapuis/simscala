package com.jcp.simscala.event

import com.jcp.simscala.event.Event.EventName
import com.jcp.simscala.resource.Resource
import com.markatta.timeforscala.Instant

sealed trait ResourceEvent[R <: Resource] extends Event {
  def resource: R
  def process: Process
  def time: Instant
}

case class ResourceRequest[R <: Resource](resource: R, process: Process, time: Instant) extends ResourceEvent[R] {
  def name: EventName = s"request: ${resource.name}"
}

case class ResourceAcquired[R <: Resource](resource: R, process: Process, time: Instant) extends ResourceEvent[R] {
  def name: EventName = s"acquire: ${resource.name}"
}

case class ResourceRelease[R <: Resource](resource: R, process: Process, time: Instant) extends ResourceEvent[R] {
  def name: EventName = s"release: ${resource.name}"
}
