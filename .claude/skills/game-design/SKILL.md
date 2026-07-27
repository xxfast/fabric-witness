---
name: game-design
description: Design, implement and verify a game mechanic in this mod as one loop. Use when the user asks to "design a mechanic", "workshop a feature", "extend this rule", "change how something works in game", or names a rule in rules/ to rework. Writes the design into the rule doc first, gets sign-off, ships the smallest working slice, then verifies it in game with screenshots before calling it done.
---

# Game design loop

Design → sign-off → implement → verify in game → fold the findings back into the doc.

The rules in `rules/` are the design surface. Every mechanic already has (or gets) one file, and that
file is what gets edited. New mechanics are almost always extensions of an existing rule, so **extend
the existing file** rather than adding a new one. Only add a file for a genuinely separate mechanic,
and index it in `rules/README.md` plus the relevant category README.

## Phase 1: learn the rule as it stands

Before proposing anything, read all of:

- `rules/README.md`, then the category index (`rules/minecraft/README.md` or `rules/witness/README.md`),
  then the specific rule file.
- The code it points at in its **Sources** section, including the tests.
- Sibling rules it links to. Mechanics here are coupled, often through a shared resource, so a
  change to one can silently make another pointless.

State what the current rule actually is before proposing a change. If the existing doc is wrong about
the code, say so; that is a finding in itself.

## Phase 2: design, and stop

The design half of a rule file is **implementation agnostic**. No class names, no method names, no
Minecraft internals, no Kotlin. Describe what a player does and what they get. All of that belongs in
the implementation half, below a `---` and an `# Implementation` heading.

Structure a reworked rule file as:

```
intro (what it is, in a sentence or two)
---
# Design
## The rule            <- the general form, then worked examples
## <mechanic sections>
## Cost                <- if it spends or returns a shared resource
## Edge cases          <- player-facing only
---
# Implementation
## Status in this mod  <- what ships today, plus known gaps
## <how it works>      <- match conditions, mappings, traps
## Not done
## Sources
```

Quantities are in the unit the player sees, not the unit the code stores. Each category README
defines its own; check it before writing numbers.

For diagrams and notation, copy whatever the neighbouring rule files already do rather than inventing
a style. State the general form once, then give worked examples. IntelliJ's markdown preview renders
LaTeX `$$` blocks, so they are available if a formula needs them.

### Then stop

Present the design and the open questions, and wait. Do not start implementing because the design
looks obviously right. The user says when it is agreed.

Before presenting, do the arithmetic on the economy and report what it implies, especially:

- Is any existing rule now dominated, i.e. strictly worse than some chain of the new one?
- Can a player recover more resources than they spent by any route? That is a dupe, not a discount.
- What is the cheapest path to the best outcome, and is that path the tedious one?

These are the findings the user cannot get from reading the diff, and they are the whole point of
designing first.

## Phase 3: implement the smallest shippable slice

Order the work so each step stands alone, and say which steps you are doing now. Prefer the slice
that lets the user *feel* the mechanic over the slice that finishes it.

Hard rules:

- **Keep every existing test case verbatim** as a regression net. If the new general rule is correct,
  the old hardcoded cases still pass unchanged, and that is the proof that the change is a
  generalisation rather than a rewrite. If a case has to change, that is a design decision to raise,
  not a test to edit.
- Pure logic goes somewhere unit tests can reach without bootstrapping Minecraft, split out of
  whatever game class holds it.
- Registration side effects must stay reachable from `onInitialize`. See `CLAUDE.md`.
- Run `./gradlew build` (compiles, and runs the tests) before handing back.

## Phase 4: verify

A green build verifies the data, not the mechanic. Say how the user can feel the change themselves,
and which cases are worth trying, including the ones you expect to fail.

If nothing about the change is visual, the tests and the build are the verification. If it does alter
something on screen, that part is **not verified until it has been seen in game**: use the
`inspect-screenshot` skill to get and read the image.

> Do not derive on-screen orientation from render transforms. The same geometry is drawn by several
> views (item icon, block entity, GUI widget) that do not share a mirror convention, so reading the
> matrices predicts the wrong answer. Look instead.

When what you see contradicts the code, **what you see wins**. Say plainly that the earlier reasoning
was wrong, work out the mapping the evidence actually implies using every case including the ones that
already looked right, fix it, and **pin the observed behaviour in a test**: comment each assertion
with the physical case it came from, the correct ones included, so nobody "fixes" them later.

Do not change renderers on inference. A rendering inconsistency found along the way gets reported,
not opportunistically fixed.

## Phase 5: fold it back

- Update the rule file's implementation half to describe what now ships, in present tense.
- Record traps that cost real time in-line, with a "do not re-derive this" warning where reasoning
  from the code gives the wrong answer.
- Keep a **Not done** section listing what is deliberately unfinished.
- Record design-level problems (a dominated rule, pricing that punishes the intended play pattern)
  as open questions in the doc rather than quietly leaving them out.
- Update `rules/README.md` and the category README if the mechanic's status changed.

Then report pitfalls to the user, ranked worst first, split into design / implementation / unknowns.
Be concise and do not soften them.

## What this skill does not do

- Does not skip the sign-off gate between design and implementation.
- Does not claim a visual behaviour works without having seen it.
- Does not commit unless asked, and never pushes (see `~/.claude/CLAUDE.md`).
