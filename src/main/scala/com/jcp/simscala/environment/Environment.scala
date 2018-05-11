package com.jcp.simscala.environment

import com.jcp.simscala.event.Event

trait Environment {
  def run(until: Option[StopCondition])
  def rewind(to: StopCondition): Event
  def pause()
  def resume()
}
