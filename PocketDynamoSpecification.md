# PocketDynamo wire protocol

Status: single-node, line-based, text protocol. This document describes
**actual behaviour traced from the code**, not intended behaviour. Where the
two differ, the gap is listed at the bottom rather than silently corrected.

## Transport and framing

- TCP, port `2001`.
- One request per line; one response per line.
- A line ends at `\n`. `\r\n` is also accepted — `BufferedReader.readLine()`
  strips both, so no `\r` reaches the parser.
- Requests are parsed with `split(" ", 3)`: at most three parts — command,
  key, and everything remaining as the value.
- Responses are written and flushed immediately, one line at a time.
- Each connection has its own socket, reader, writer, and handler thread.

## Commands

| Command | Form | Response on success |
| --- | --- | --- |
| `SET` | `SET <key> <value>` | `OK` |
| `GET` | `GET <key>` | the stored value, or an empty line |
| `DEL` | `DEL <key>` | `OK` |
| `EXIT` | `EXIT` | `OK`, then the connection closes |

Commands are **case-sensitive**. `set` is not a valid command.

## Key and value rules

- A key may not contain a space, because the third split boundary falls after
  the key.
- A value **may** contain spaces. `SET k hello world` stores `hello world`.
- A value is everything after the first space following the key, taken
  verbatim. It is never trimmed. `SET k  hello` (two spaces) stores
  `" hello"`, with the leading space preserved.
- An empty or whitespace-only key is rejected on `SET`.
- An empty value is currently accepted. `SET k ` stores the empty string.

## Error responses

Errors are returned as a single line. The connection stays open.

| Condition | Response |
| --- | --- |
| `SET` with empty or missing key | `Key cannot be empty` |
| `SET` with no value | `SET requires key and value` |
| `GET` with more than one argument | `GET does not accept a value` |
| `GET` or `DEL` with no key | `GET requires a key` / `DEL requires a key` |
| Unknown command | `No enum constant org.db.Command.<input>` |
| Empty line | `No enum constant org.db.Command.` |

## Connection lifecycle

- `EXIT` closes the requesting connection only. The server keeps running and
  other clients are unaffected.
- A client disconnecting without `EXIT` is handled the same way: the read
  returns end-of-stream, the handler exits its loop, and the socket is closed.
- A rejected command does not close the connection.

## Decisions and what they cost

**Rest-of-line values.** Chosen over a length-prefixed format because it needs
no escaping and no framing logic. The cost is that a value can never contain a
newline, and leading spaces after the key are indistinguishable from part of
the value. A length prefix is the eventual fix.

**Protocol format and storage format are the same string.** A write-ahead-log
record is the command line as received, so `Operation.parse` is used both for
live traffic and for replay on restart. This makes a spaced value impossible to
interpret two different ways. The cost is coupling: changing the wire format
changes the on-disk format, and vice versa.

**Reject rather than normalize.** Malformed whitespace produces an error
instead of being trimmed into something valid. Trimming would have to decide
what happens to spaces inside a value, which would couple parsing to value
semantics.

## Known gaps

These are real and not yet decided. They are listed so the document is not
mistaken for a specification of correct behaviour.

1. **Unknown commands leak a JDK internal message.** `Command.valueOf` throws
   with the text `No enum constant org.db.Command.FOO`, and that string is
   returned to the client verbatim. The `"Invalid command"` fallback in
   `Operation.parse` is unreachable for this case, because the JDK exception is
   already an `IllegalArgumentException` and is rethrown unchanged.

2. **`DEL` does not reject an empty key.** The empty-key check was added to
   `SET` only. `DEL  A` (two spaces) parses to an empty key, deletes nothing,
   and still answers `OK`.

3. **`DEL` ignores extra arguments.** `DEL a b` deletes `a` and discards `b`
   without complaint.

4. **`EXIT` ignores extra arguments.** `EXIT a b` is accepted and closes the
   connection.

5. **A missing key and a stored empty string are indistinguishable.** Both
   answer with an empty line. Either empty values must be rejected or the
   missing-key response must change.

6. **Durability is not established.** Records reach the operating system via
   `flush()` but are never forced to disk, and the in-memory map is updated
   before the record is written. Acknowledged writes survive a process kill,
   not a power loss.

7. **The log writer is shared across threads without coordination.** The
   `write` / `newLine` / `flush` sequence is three separate operations on one
   static writer, so concurrent clients can interleave into a corrupt record.

8. **Recovery aborts on a malformed record.** Replay calls the same parser as
   live traffic, and a truncated final line throws out of startup rather than
   being discarded.