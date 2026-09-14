# PocketDynamo

> Working title for a Java key-value-store learning project.

PocketDynamo is an early-stage key-value server built to explore storage-engine and distributed-systems fundamentals through working code. The current version is a single-node TCP server. The intended direction is a durable, Dynamo-inspired distributed key-value store for small application objects.

This repository is a learning prototype, not production-ready software.

## Current capabilities

- Line-based TCP protocol on port `2001`
- `SET`, `GET`, `DEL`, and `EXIT` commands
- Concurrently connected clients
- One shared in-memory `ConcurrentHashMap`
- Experimental write-ahead-log and startup-recovery code

The project is not currently a cache: it has no TTL, eviction policy, or external source-of-truth integration.

## Requirements

- JDK 25
- A TCP client such as `nc` (netcat)

## Compile and run

From the repository root:

```bash
mkdir -p target/classes
javac -d target/classes src/main/java/org/db/Main.java
java -cp target/classes org.db.Main
```

Expected startup output:

```text
DB Starting up ....
DB Started
Waiting for connection
```

Connect from another terminal:

```bash
nc localhost 2001
```

## Protocol

Commands and arguments are separated by spaces.

| Command | Example | Current response |
| --- | --- | --- |
| Set a value | `SET NAME RACHIT` | `OK` |
| Read a value | `GET NAME` | Stored value |
| Delete a value | `DEL NAME` | `OK` |
| Close one client connection | `EXIT` | `OK` |

A missing key currently returns an empty line. `EXIT` closes the requesting client connection; it does not stop the server.

## Request path

1. The server listens on port `2001` and accepts a socket.
2. Each accepted connection gets a handler thread.
3. The handler reads one command line through its client-specific `BufferedReader`.
4. The command is parsed and validated.
5. The operation runs against the shared map.
6. The result is written and flushed through that client's `BufferedWriter`.

Each client has its own `Socket`, reader, writer, and handler thread. All client handlers currently share the same `ConcurrentHashMap` and WAL writer.

## Verified baseline

On 14 September 2026, the server was compiled and launched with the commands above. Two simultaneous netcat clients demonstrated shared state:

1. Client A stored `NAME = RACHIT`.
2. Client B read `RACHIT`.
3. Client A overwrote the value with `UPDATED`.
4. Client B read `UPDATED`.
5. Client A deleted the key.
6. A later read returned the current missing-key response: an empty line.

## Known limitations

- The WAL implementation is experimental and must not yet be treated as durable.
- WAL writes and recovery need a single consistent text-record format.
- WAL access is not safely coordinated across concurrent client handlers.
- The server creates an unbounded platform thread per accepted connection.
- Command parsing does not support values containing spaces.
- Protocol edge-case behaviour is not fully specified or automatically tested.
- There is no partitioning, replication, membership, or node-failure handling.
- Compilation currently reports an unchecked-operation warning from a raw generic construction.

## Roadmap

1. Define protocol edge behaviour and add automated socket-level checks.
2. Implement a correct append-only WAL and restart recovery.
3. Introduce bounded connection handling and explicit resource limits.
4. Separate protocol, server, storage, and persistence responsibilities.
5. Partition keys across local nodes with consistent hashing.
6. Add replication and demonstrate behaviour during node failure.
7. Add measurements, architecture documentation, and a reproducible showcase workload.

The final name and concrete demonstration workload will be locked before the distributed milestone.
