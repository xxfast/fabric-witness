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
| `panel_scint_startpoint` | 0.20 | pitch-randomised, see below |
| `panel_scint_endpoint` | 0.15 | pitch-randomised |
| `panel_success` | 0.30 | 4 variants, picked at random |
| `panel_success_muted` | 0.70 | louder scale, quieter source |
| `panel_failure` | 0.30 | |
| `panel_potential_failure` | 0.40 | |
| `panel_path_complete` | 0.15 | |
| `focus_mode_enter` / `focus_mode_exit` | 0.20 | |
| `focus_mode_being` / `focus_mode_doing` | 0.13 | |
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

Registered in `WitnessSounds.kt`:

- `panel_start_tracing`
- `pointless_click`
- all six `focus_mode_*`

Not yet implemented, roughly in order of how much they'd add:

| event | why it matters |
|---|---|
| `panel_success` | the signature moment; wants 4 variants |
| `panel_failure` | no feedback on invalid submission today |
| `panel_finish_tracing` | completes the start/finish pair we already half-have |
| `panel_abort_tracing` | feedback when a trace is dropped |
| `panel_scint_startpoint` / `_endpoint` | node affordance — helps discoverability |
| `panel_potential_failure` | teaches constraints while drawing |
| `panel_path_complete` | separates "reached exit" from "solved" |
| `panel_abort_finish_tracing` | polish |
| `panel_success_muted` | only once panels can cluster |

Each needs an entry in `sounds.json`, a `SoundEvent` in `WitnessSounds.kt` registered during
common init (registries freeze afterwards), and a trigger in the puzzle solver.
