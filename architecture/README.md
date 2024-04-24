# Gradle architecture documentation

This directory contains documentation that describes Gradle's architecture and how it works.

## Architecture decision records (ADRs)

The Gradle team uses ADRs to record architecture decisions that the team has made.

See [Architecture decisions records](standards) for the list of ADRs.

## Platform architecture

Gradle is arranged into several coarse-grained components called "platforms".
Each platform provides support for some kind of automation, for example building JVM software or building Gradle plugins. Most platforms typically build on the features of other platforms.

By understanding the Gradle platforms and their relationships, you can get a feel for where in the Gradle source a particular feature might be implemented.

See [Gradle platform architecture](platforms.md) for a list of the platforms and more details about how they work.
