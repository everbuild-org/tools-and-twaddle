---
title: Architecture
description: How Tools & twaddle divides framework-wide responsibilities from optional server features.
---

Tools & twaddle is a layer on Minestom, not a replacement for it. Applications continue to create Minestom servers,
inventories, event nodes, and items directly.

## Dependency direction

```text
application
   ├─ depends on → twaddle-core
   ├─ depends on → selected feature modules
   └─ depends on → Minestom

feature module
   ├─ may depend on → twaddle-core
   └─ integrates with → Minestom

twaddle-core
   └─ extends → Minestom primitives
```

The application is the composition root. It chooses modules and controls Minestom initialization. Modules should not
need a central registry merely to exist.

## What belongs in the foundation

Foundation code must represent a concern shared by essentially every server or required by many unrelated modules:

- application lifetime and resource ownership;
- coroutine lifetime;
- framework-wide observability conventions;
- tiny Minestom interoperability helpers.

Gameplay rules, content systems, persistence, commands, and other optional behavior belong in modules. Their public API
should normally live with the module that owns the concept.

## Lifecycle model

`TwaddleContext` is both a `CoroutineScope` and a lifetime owner. Installation code can launch work in that scope, hand
it `AutoCloseable` resources, and create named child contexts with `fork`. A parent shuts down its children alongside
its other owned work, in reverse acquisition order.

`initialized(server)` exposes the same lifetime as a `TwaddleServerContext`. Modules that run after Minestom starts can
accept that narrower type when they require a `MinecraftServer`.

Shutdown proceeds in a fixed order:

1. reject and close newly offered resources;
2. cancel and join lifetime coroutine work;
3. shut down child contexts and close resources in reverse acquisition order;
4. preserve the first close failure and suppress subsequent failures.

`AfterInit<A>` describes work prepared before Minestom initialization that needs a `MinecraftServer` to finish. It is a
small lifecycle vocabulary type, not a module container.
