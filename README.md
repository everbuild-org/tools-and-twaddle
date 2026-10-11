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

## Who this is for
Tools and twaddle is a good fit if you:
- Build directly against Minestom.
- Prefer explicit architecture to magic.
- Want a modular server codebase.
- Are comfortable working with unstable APIs in exchange for stronger structure.
- Enjoy experimenting with framework design in public.

It is probably not the right fit if you need a frozen API surface, broad ecosystem compatibility, or a drop-in
replacement for established server platforms.

## Rule stacking

The rule-stacking API is available in two artifacts:

- `twaddle-stacked-behaviour` provides activity contexts, rules, chains, the editing DSL, and the execution engine in
  `org.everbuild.twaddle.stacked_behaviour`.
- `twaddle-stacked-behaviour-block` provides block rulesets, interaction contexts, and block handlers in
  `org.everbuild.twaddle.stacked_behaviour_block`. It exposes the general API through an `api` dependency.

These replace `twaddle-stacked-block-behaviour` and its `org.everbuild.twaddle.stacked_block_behaviour` package.

`StackedRuleset<R, E>` maps subject identifiers to immutable `Behaviour<R>` definitions. Each behaviour binds typed
`ActivityKey<R, C>` tokens to rule chains for context `C`. `RulesetEditor` and `BehaviourEditor` provide the shared
editing API; `BlockRulesetEditor` provides block selection as instance methods in the block package.

```kotlin
import net.minestom.server.instance.block.Block
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivity

val ruleset = BlockRuleset.build {
    addBlock(Block.STONE) {
        activity(InteractionActivity).replace {
            install(Rule(Rule.Key("example:replace_stone")) { context ->
                context.replaceBlock(Block.DIAMOND_BLOCK)
                RuleResult.consume()
            })
        }
    }
}

val modified = ruleset.modify {
    blocks(Block.STONE) { activity(InteractionActivity).clear() }
}
```

Every chain editing verb is available through `activity(key)`, including `append`, `prepend`, `before`, `after`,
`replace`, and `remove`. `omit()` removes the activity contribution; `clear()` contributes an explicit empty chain.
Combining rulesets permits different activities on the same subject and rejects overlapping activity contributions.

Use `ruleset.modify { retainOnly(Block.STONE, Block.DIRT) }` to keep selected blocks. The general editor accepts `Key`
identifiers. Retained behaviours keep their original order; absent identifiers are ignored, and `retainOnly()` clears
the ruleset. The original ruleset is unchanged.

Kotlin callers can import `org.everbuild.twaddle.stacked_behaviour_block.type.interact` to use
`interact.append { ... }` as shorthand for `activity(InteractionActivity).append { ... }`.

Declare activity keys once and reuse them: keys use identity equality, with their textual IDs serving as diagnostic
labels. A key's ruleset parameter restricts its domain, and its context parameter determines which rules can be installed.
For another domain, define a ruleset type such as `class ItemRuleset : StackedRuleset<ItemRuleset, RulesetEditor<ItemRuleset>>(emptyMap(), ::RulesetEditor)`, then use
`StackedRuleset.build<ItemRuleset> { add(itemKey) { activity(itemActivity).append { /* install rules */ } } }`.

Java callers use static `BlockRuleset.build` and `Consumer` callbacks with the concrete editor:

```java
var ruleset = BlockRuleset.build(editor -> {
    editor.addBlock(Block.STONE, behaviour -> behaviour.activity(myActivity).clear());
});
var modified = ruleset.modify(editor -> {
    editor.blocks(behaviour -> behaviour.activity(myActivity).omit(), Block.STONE);
    editor.removeBlocks(Block.STONE);
});
```

The Java `blocks` overloads take the callback first so the selected keys or blocks remain true varargs.

An integration compiles a selected activity with `behaviour.createApplicator(activityKey, factory)` and supplies its
context to `apply`. Missing activities fall through. The block integration's `createHandlers` selects `InteractionActivity`;
installation onto blocks and registration with Minestom remain the caller's responsibility. Custom activity dispatch
belongs to the integration that owns that activity.

## Random ticking

`twaddle-random-ticking` exposes `twaddle-common-block` through an `api` dependency and dispatches random ticks to
block handlers implementing `RandomTickHandler`.

```kotlin
import net.minestom.server.MinecraftServer
import org.everbuild.twaddle.random_ticking.getRandomTickingEventNode
import org.everbuild.twaddle.random_ticking.randomTickSpeed

MinecraftServer.getGlobalEventHandler().addChild(getRandomTickingEventNode())
// Configure each instance through its random tick speed tag:
// instance.randomTickSpeed = 4
```

The default speed is 3 samples per section per instance tick. Only chunks with all eight neighbouring chunks loaded
participate. A handler's non-null result replaces the sampled block; null leaves it unchanged.

## Contributing

Feedback is most useful when it comes from concrete use cases. Bug reports, architectural criticism, API proposals, and
small reference implementations are all valuable while the project direction is still flexible.

If you are building something on top of Tools and twaddle, document the friction points. Those rough edges are the point
of an experimental framework: they show what the core should absorb and what should stay outside it.

## Name
Yes, the name is a bit cheeky. The framework is serious about architecture, even if the branding refuses to act like it.
