# Panel audio — event model

Design notes for the puzzle-panel sound layer: which cue fires when, how loud, and whether it
is positional. The event vocabulary and mix values below are observations of how the original
game structures its panel audio, recorded here so our own sounds can be authored and wired to
match. **No game audio is described or included here** — this is the behavioural model only.

## The event set

Nine panel events, as four related pairs plus the resolution cues:

| event | fires when |
|---|---|
| `panel_start_tracing` | the player begins a trace from a start node |
| `panel_finish_tracing` | the trace is released on a valid endpoint |
| `panel_abort_tracing` | the trace is cancelled mid-line |
| `panel_abort_finish_tracing` | the trace is cancelled *after* having reached the finish |
| `panel_scint_startpoint` | the start node shimmers (hover / attract) |
| `panel_scint_endpoint` | the end node shimmers |
| `panel_success` | validation passed |
| `panel_failure` | validation failed |
| `panel_potential_failure` | interim warning — the path in progress cannot satisfy a constraint |

Plus two that behave differently (see *Variants by surface*):

| event | fires when |
|---|---|
| `panel_path_complete` | the line reaches the exit, before release/validation |
| `panel_success_muted` | a quieter success, for panels that shouldn't dominate the mix |

The distinction between `finish_tracing`, `path_complete` and `success` is the useful one:
`path_complete` is geometric (you touched the exit), `finish_tracing` is the release gesture,
and `success` only follows after the puzzle rules are checked. `potential_failure` is the
"this is already wrong" nudge while still drawing.

## Mix levels

Volume scale per event, on a 0–1 scale:

| event | volume | notes |
|---|---|---|
| `panel_start_tracing` | 0.40 | |
| `panel_finish_tracing` | 0.20 | |
| `panel_abort_tracing` | 0.40 | |
| `panel_abort_finish_tracing` | 0.30 | |
| `panel_scint_startpoint` | 0.20 | pitch-randomised, see below; we run it at 0.12 |
| `panel_scint_endpoint` | 0.15 | pitch-randomised |
| `panel_success` | 0.30 | 4 variants, picked at random |
| `panel_success_muted` | 0.70 | louder scale, quieter source |
| `panel_failure` | 0.30 | |
| `panel_potential_failure` | 0.40 | |
| `panel_path_complete` | 0.15 | |
| `focus_mode_enter` / `focus_mode_exit` | 0.20 | |
| `focus_mode_being` | 0.13 | we run it at 0.05, see below |
| `focus_mode_doing` | 0.13 | |
| `focus_mode_wondering` | 0.17 | |
| `focus_mode_considering_exit` | 0.17 | |
| `pointless_click` | 0.30 | |

Two variation parameters matter beyond volume:

- **Variant count.** `panel_success` has four alternates chosen at random, so a solve doesn't
  sound identical every time. Everything else is single.
- **Random pitch**, in semitones. The `scint_*` cues are pitch-jittered per play (up to
  ~±0.9 semitones on some surfaces) rather than being one fixed sample. Worth replicating —
  it's most of why node hover doesn't get grating.

## Positional vs. 2D — don't conflate these

The `panel_*` events above are **non-positional**: they are 2D interface cues played while the
player is focused on a panel, with no distance attenuation.

The ambient hum you hear *walking past* a panel is a separate concern with its own falloff
radii. Keep the two layers distinct — a solve chime should not attenuate with distance, and an
idle hum should not play into the focused mix at full volume.

For reference, a positional cue carries inner/outer radii (e.g. 15 / 200 world units) where
these interface events carry none.

## Focus mode

"Focus mode" is the zoomed-in panel-solving state — the panel fills roughly 0.6 of the screen
and player movement is suspended. `focus_mode_enter` and `focus_mode_exit` bracket it.

The remaining four are ambient layers *within* focus mode rather than one-shot events:

| layer | reading |
|---|---|
| `focus_mode_being` | idle — focused but not drawing |
| `focus_mode_doing` | actively tracing |
| `focus_mode_wondering` | hesitating / hovering without committing |
| `focus_mode_considering_exit` | drifting toward leaving |

These four are an interpretation from naming and grouping, not a confirmed state machine —
treat the exact transitions as ours to define.

Related behaviours worth mirroring: a hint appears after ~4s of trying to leave, focus exits
automatically once a multi-panel is completed, and panels within ~2.5 blocks pull focus.

## Variants by surface

Seven of the nine events support a per-surface variant, selected by the acoustic zone the
panel stands in — so a panel in a glass workshop and one in a stone corridor resolve with
different reverb treatments of the same cue. The name is composed at runtime as
`<zone>_panel_<event>`.

`panel_path_complete` and `panel_success_muted` are **not** part of that scheme and always use
the single base cue.

If we implement zones, note that in the original the default zone accounts for the large
majority of panels — the exotic treatments appear only a handful of times each. Build the
default path first.

## Current state in this mod

Every cue is a `WitnessSound` in `WitnessSounds.kt`, which pairs the registered `SoundEvent` with
its mix from the table above, so the volume and pitch jitter live in one place rather than at each
call site. Registration happens during common init: registries freeze afterwards.

Wired up, all of it in `PuzzleSolverScreen`:

| event | trigger |
|---|---|
| `focus_mode_enter` / `_exit` | the solver screen opens / closes |
| `focus_mode_being` | looping, whenever the screen is open and no line is moving |
| `focus_mode_doing` | looping, replaces `being` for the duration of a trace |
| `panel_scint_startpoint` | the cursor enters a start node, after 5s of the panel going untouched |
| `panel_scint_endpoint` | a trace has been stalled for 5s, repeating on that beat |
| `panel_start_tracing` | a trace begins on a start node |
| `panel_path_complete` | the line lands on an end point, whatever the path is worth |
| `panel_finish_tracing` | the line is released on an end point, before the verdict |
| `panel_success` | validation passed, one of the four variants at random |
| `panel_failure` | validation failed |
| `panel_abort_tracing` | the line is released anywhere else, or the trace is dropped |
| `panel_abort_finish_tracing` | the trace is dropped while resting on an end point |
| `pointless_click` | a click that hits no start node |

Both `scint_*` cues are attract cues on the same five second timer, but they answer different
questions. `startpoint` fires when the cursor enters a start node on a panel nobody has touched
for five seconds: where do I begin. `endpoint` fires when a trace has been running but the line
has not moved for five seconds, and repeats on that beat until it does: where am I going. Clicks
and a moving line count as touching the panel, drifting the cursor across it does not, otherwise
the move onto a node would reset the timer the cue is waiting on.

Neither cue is tied to hovering the thing it points at. Nothing found in datamined panel data
supports a tutorial versus non-tutorial split in the original, where these would be taught and
then withdrawn, so they stay on every panel and lean on the idle gate instead. If panels ever grow
a per-panel hinting flag, this is where it would apply.

`focus_mode_being` deviates from the table: 0.05 rather than 0.13, and eased in over three seconds
by `LoopingSoundInstance` instead of starting at full volume. It is the one layer that runs the
whole time a panel is open, and at the observed level it pulls attention off the puzzle. The fade
curve is squared, since a linear ramp is most of the way up almost immediately.

Only real verdicts get verdict cues. Releasing off an end point is an *abort*, not a rejection:
`PuzzleSolver.submit` only reaches validation from an end point, so a dropped line never sounds
like a wrong answer, and `failure` only fires on a path the panel actually rejected. Nothing
predicts a verdict ahead of the release.

Not wired up yet, and why:

| event | blocked on |
|---|---|
| `panel_potential_failure` | reserved for eliminators (rules/witness/11-eliminators.md), the rule that fails visibly mid-trace |
| `focus_mode_wondering` / `_considering_exit` | no hesitation or exit-drift state to hang them on |
| `panel_success_muted` | only earns its place once panels can cluster |
| `<zone>_panel_*` (`crt`, `defaultverb`, `glassverb`) | no acoustic zone concept — see *Variants by surface* |
| `menu_*` | the composer GUI has no sound layer |

The files ship regardless; wiring one up is an entry in `sounds.json`, a `WitnessSound` in
`WitnessSounds.kt`, and a trigger. The uncompressed sources in `raw/` and `pixelated/` are
gitignored working files and are excluded from the jar by `processResources`.
