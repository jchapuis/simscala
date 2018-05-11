package com.jcp.simscala.context

import com.markatta.timeforscala.Instant

case class SimTime(now: Instant, initialTime: Instant)

object SimTime {
  val epoch = SimTime(Instant.Epoch, Instant.Epoch)
}
