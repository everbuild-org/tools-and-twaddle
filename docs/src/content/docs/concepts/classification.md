---
title: Capability classification
description: A practical vocabulary for locating, describing, and maturing framework capabilities.
---

Every capability can be described on three independent axes. A label should answer **where it belongs**, **what it
does**, and **how safe it is to adopt**.

## 1. Placement

| Label | Meaning | Dependency expectation |
| --- | --- | --- |
| **Foundation** | Universal process or framework concern | Lives in `twaddle-core` |
| **Bridge** | Small adaptation of a Minestom primitive | Lives in core only when broadly reused; otherwise beside its module |
| **Module** | Optional server or gameplay capability | Separate Gradle module with its own API |

Placement is about ownership, not importance. A large, essential gameplay system can still be a module.

## 2. Concern

Use a concrete domain label such as **Runtime**, **Observability**, **Interop**, or **Gameplay policy**. New labels should
describe a user-visible responsibility rather than an implementation technology.

## 3. Maturity

| Label | Promise |
| --- | --- |
| **Experimental** | Works and is tested, but compatibility is not promised |
| **Incubating** | Shape exists, but an important execution path is incomplete |
| **Supported** | Documented behavior with an intentional compatibility policy |

Nothing is classified as Supported yet.
