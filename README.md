# Tools and twaddle

> An opinionated, experimental server framework built around minestom

**Tools and twaddle** is a pluggable server-side modding framework for Minestom, built for developers who want a more
structured foundation than raw Minestom without giving up the flexibility that makes Minestom appealing. Minestom itself
is intentionally lightweight, extensible, and designed for high performance with minimal overhead, which makes it a
strong base for opinionated frameworks layered on top of it.

Created by the makers of [blocks-and-stuff](https://github.com/everbuild-org/blocks-and-stuff), this project explores a
different part of the Minecraft server stack: not vanilla gameplay but the addition of new game elements into minestom.

## Status
This project is **experimental**. The APIs, lifecycle model, module boundaries, and extension patterns should be
expected to change quickly while the framework direction is being refined.

Tools and twaddle is meant for developers who are comfortable evolving abstractions, want to shape the framework
through real-world usage, and prefer explicit trade-offs to broad compatibility promises.

## Goals
Tools and twaddle is built around a few strong opinions:
- Minestom should stay the runtime, not disappear behind a giant abstraction layer.
- Common server concerns should be pluggable instead of hand-rolled in every project.
- Modules should compose cleanly through explicit contracts and lifecycle hooks.
- Server features should be isolated enough to develop, test, and replace independently.
- The framework should make it easier to build actual projects, not just quick prototypes.

## What it provides
While the exact feature set depends on the current implementation, the intended shape of the framework is:
- A DI context for building and testing server software based on koin
- A structured bootstrap and lifecycle model for different elements
- Shared conventions for configuration, service registration, and cross-module communication.
- An opinionated developer experience on top of Minestom's low-level primitives.


## Design principles

### Pluggable by default
Features should live in modules, not in a single inseparable core. If a project does not need a subsystem, it should be
possible to leave it out.

### Opinionated where it matters
The framework should reduce decision fatigue around project structure, lifecycle, and integration patterns. The goal is
not to support every architecture equally well, but to make one coherent architecture productive.

### Server-side first
Tools and twaddle focuses on server-side modding and custom gameplay infrastructure. It is not trying to mimic a client
mod loader; it is trying to give Minestom-based servers a better composition model.

### Built for codebases, not demos
The framework should scale from isolated experiments to larger projects with multiple systems, contributors, and
deployment environments.

## Getting started
Until the public API settles, the best way to approach Tools and twaddle is as a framework for experimentation:
1. Start with a small Minestom project.
2. Add Tools and twaddle as the organizational layer around it.
3. Keep gameplay logic in isolated modules.
4. Expect breaking changes while the architecture matures.
5. Use the project to validate patterns before committing to long-term APIs.

## Who this is for
Tools and twaddle is a good fit if you:
- Build directly against Minestom.
- Prefer explicit architecture to magic.
- Want a modular server codebase.
- Are comfortable working with unstable APIs in exchange for stronger structure.
- Enjoy experimenting with framework design in public.

It is probably not the right fit if you need a frozen API surface, broad ecosystem compatibility, or a drop-in
replacement for established server platforms.

## Contributing
Feedback is most useful when it comes from concrete use cases. Bug reports, architectural criticism, API proposals, and
small reference implementations are all valuable while the project direction is still flexible.

If you are building something on top of Tools and twaddle, document the friction points. Those rough edges are the point
of an experimental framework: they show what the core should absorb and what should stay outside it.

## Name
Yes, the name is a bit cheeky. The framework is serious about architecture, even if the branding refuses to act like it.