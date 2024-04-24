# Gradle architecture documentation

This directory contains documentation that describes Gradle's architecture and how the various pieces fit together and work.

## Architecture decision records (ADRs)

The Gradle team uses ADRs to record architecture decisions that the team has made.

See [Architecture decisions records](standards) for the list of ADRs.
Be aware these are very technical descriptions of the decisions, and you might find the documentation below more useful as an introduction to the internals of Gradle.

## Platform architecture

Gradle is arranged into several coarse-grained components called "platforms".
Each platform provides support for some kind of automation, for example building JVM software or building Gradle plugins.
Most platforms typically build on the features of other platforms.

By understanding the Gradle platforms and their relationships, you can get a feel for where in the Gradle source a particular feature might be implemented.

See [Gradle platform architecture](platforms.md) for a list of the platforms and more details about how they work.

## Gradle runtimes

At runtime, Gradle is made up of several different processes that work together to "run the build". For example, the Gradle daemon or the `gradlew` command.

Each process, or "runtime", applies different constraints to the code that runs in that process. For example, each process has different supported JVMs and a different set of services available for dependency injection.
While most Gradle source code runs in the Gradle daemon, it is still useful to be aware of the processes in which a particular piece of code will run.

See [Gradle runtimes](runtimes.md) for a list of these processes and more details about how they work.
