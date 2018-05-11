package com.jcp.simscala.environment

object EnvironmentCommands {
  sealed trait EnvironmentCommand
  case class RunCommand(until: Option[StopCondition]) extends EnvironmentCommand
  case object PauseCommand                            extends EnvironmentCommand
  case object ResumeCommand                           extends EnvironmentCommand
  case class RewindCommand(to: StopCondition)         extends EnvironmentCommand
}
