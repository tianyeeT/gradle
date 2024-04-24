# Gradle runtimes

Gradle is made up of the following processes:

- Gradle daemon. This is the process where Gradle runs the build. It is a long-running daemon process.
- CLI client. This is the `gradle` or `gradlew` command, and is responsible for locating, starting and interacting with the Gradle daemon. 
- Tooling API client. This a library that is embedded into applications, such as IDEs or CI agents, that allows them to act as a Gradle client.
- Worker processes. Daemon processes that the Gradle starts to run specific kinds of work, such as compilation or test execution.

These are all Java processes.

```mermaid
    graph TD
    
    cli["CLI client"]
    
    tapi["Tooling API client"]
    
    daemon["Gradle daemon"]
    cli --> daemon
    tapi --> daemon
    
    worker["Worker process"]
    daemon --> worker
```
