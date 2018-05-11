package com.jcp.simscala.event

import com.markatta.timeforscala.Instant

case class ProcessEnd[T](process: Process, time: Instant, value: T) extends Event with HasValue[T] {
  def name = process.name + "_end"
}
