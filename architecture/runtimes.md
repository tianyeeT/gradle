# Gradle runtimes

Gradle is made up of the following processes:

- Gradle daemon. This is the process where Gradle runs the build. It is a long-running daemon process.
- CLI client. This is the `gradle` or `gradlew` command, and is responsible for locating, starting and interacting with the Gradle daemon. 
- Tooling API client. This a library that is embedded into applications, such as IDEs or CI agents, that allows them to act as a Gradle client.
- Worker processes. Daemon processes that the Gradle starts to run specific kinds of work, such as compilation or test execution.
- https://services.gradle.org/. Provides information about Gradle releases and distributions.
- https://plugins.gradle.org/. The Gradle plugin portal.

```mermaid
    graph TD
    
    subgraph local["Local machine"]
        
        subgraph gradle
            cli["CLI client"]
        end
    
        subgraph gradlew
            cli_gradlew["CLI client"]
        end
    
        subgraph IDE    
            tapi["Tooling API client"]
        end
        
        daemon["Gradle daemon"]
        cli --> daemon
        cli_gradlew --> daemon
        tapi --> daemon
        
        worker["Worker process"]
        daemon --> worker

        worker2["Worker process"]
        daemon --> worker2

        worker3["Worker process"]
        daemon --> worker3
        
    end
    
    subgraph network
        services["services.gradle.org"]
        daemon --> services
    
        plugins["plugins.gradle.org"]
        daemon --> plugins
    end
    
```

These are all Java processes. All source core in Gradle is written to target one or more of these runtimes.
