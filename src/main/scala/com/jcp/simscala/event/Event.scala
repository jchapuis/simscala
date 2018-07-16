package com.jcp.simscala.event

import com.markatta.timeforscala.Instant

object Event {
  type EventName       = String
  type CallbackMessage = Any
  val Never = new Event {
    def name: EventName = "Never"
    def time: Instant = Instant.Epoch
  }
}

trait Event {
  import Event._
  def name: EventName
  def time: Instant
}
