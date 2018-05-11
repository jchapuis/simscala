package com.jcp.simscala.event

import com.markatta.timeforscala.Instant

object Event {
  type EventName       = String
  type CallbackMessage = Any
}

trait Event {
  import Event._
  def name: EventName
  def time: Instant
}
