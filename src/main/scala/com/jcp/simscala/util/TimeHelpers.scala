package com.jcp.simscala.util

import com.markatta.timeforscala.{ Instant, Millis }

object TimeHelpers {
  import PrettyDuration._
  def durationFromEpoch(instant: Instant): String =
    Millis(instant.toEpochMilli).pretty
}
