# SimScala

Process-oriented lightweight simulation framework for Scala and Akka, inspired by SimPy. Aims to provide a centrally-controlled simulation engine (master/slave) while yet allowing for a scalable simulation environment.

Processes are implemented by actors. Since Akka actors are asynchronous by nature, in contrast to SimPy's coroutines they can run in parallel, can even be distributed and interact with outside systems, and recover from failures. Simulation actors (processes) therefore have all freedom to carry asynchronous operations as much as they need (e.g. to interface with external services, load up data, etc.).

However in terms of the simulation run itself, full asynchronicity is not desirable. Indeed, time is simulated and the simulation engine needs to collect and process all events belonging to a certain time point before proceeding to the next time point.

In order to guarantee such repeatability in simulation runs, operation of the simulation itself (event generation and exchange) is therefore synchronous (using Akka ask pattern). This imposes a limit on scalability, as events are unfolded one after the other. However, to alleviate this limitation, simscala offers explicit opt-in semantics to run processes in parallel and wait for all results with a synchronization barrier.

Ordering of events is managed with a time-based priority-queue mailbox by the simulation supervising actor (the environment). Simulation runs are recorded and can be replayed thanks to Akka persistence. If simulation actors interact with outside systems, its the responsibility of the simulation actor to guarantee idempotent and stateless operation across replays.

Simscala allows for switching execution modes between discrete, real-time or delta-time. This is a configuration change and has no impact on simulation code (except when interacting with outside systems, which need to support the difference in behavior entailed by switching between these various modes)

**Work in progress**