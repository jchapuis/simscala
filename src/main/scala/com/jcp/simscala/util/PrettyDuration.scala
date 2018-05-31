package com.jcp.simscala.util

import java.util.Locale

import com.markatta.timeforscala.{ Duration, Nanos, TimeUnit }

import scala.concurrent.duration.{ DAYS, HOURS, MICROSECONDS, MILLISECONDS, MINUTES, NANOSECONDS, SECONDS }

object PrettyDuration {
  implicit class PrettyPrintableDuration(val duration: Duration) extends AnyVal {

    /** Selects most appropriate TimeUnit for given duration and formats it accordingly, with 4 digits precision **/
    def pretty: String = pretty(includeNanos = false)

    /** Selects most appropriate TimeUnit for given duration and formats it accordingly */
    def pretty(includeNanos: Boolean, precision: Int = 4): String = {
      require(precision > 0, "precision must be > 0")

      duration match {
        case d: Duration =>
          val nanos = d.toNanos
          val unit  = chooseUnit(nanos)
          val value = nanos.toDouble / NANOSECONDS.convert(1, unit)
          s"%.${precision}g %s%s".formatLocal(
            Locale.ROOT,
            value,
            abbreviate(unit),
            if (includeNanos) s" ($nanos ns)" else ""
          )
        case _ => "undefined"
      }
    }

    def chooseUnit(nanos: Long): TimeUnit = {
      val d = Nanos(nanos)

      if (d.toDays > 0) DAYS
      else if (d.toHours > 0) HOURS
      else if (d.toMinutes > 0) MINUTES
      else if (d.getSeconds > 0) SECONDS
      else if (d.toMillis > 0) MILLISECONDS
      else if (d.toNanos / 1000 > 0) MICROSECONDS
      else NANOSECONDS
    }

    def abbreviate(unit: TimeUnit): String = unit match {
      case NANOSECONDS  => "ns"
      case MICROSECONDS => "μs"
      case MILLISECONDS => "ms"
      case SECONDS      => "s"
      case MINUTES      => "min"
      case HOURS        => "h"
      case DAYS         => "d"
    }
  }

}
