---
name: inspect-screenshot
description: Capture and read in-game screenshots so what is actually on screen can be seen. Use when a question needs looking at the game rather than reading code, when the user says "check the screenshot(s)", "see for yourself", "look at this", or when a detail in an existing screenshot is too small to read at full-screen scale.
---

# Inspect screenshot

This skill gets an image and reads what is in it. It does not decide whether the thing in the image
is correct; the caller does that.

## Getting a shot

The user plays; you look. Launch:

```shell
./gradlew runClient --args='--quickPlaySingleplayer "New World"'
```

Screenshots land in `run/screenshots/`. Sort by modification time, since the newest matter most and
filenames are timestamps. `Read` them directly.

## Asking for the right shot

Say exactly what to capture. Frame the request so the image can actually answer the question:

- **Pick a discriminating subject.** Anything symmetric looks identical mirrored and unmirrored, so a
  centred subject settles nothing. Ask for something off-centre.
- **One variable at a time**, one axis at a time.
- **Ask for the control too**, the case expected to already be fine, not only the suspect one.
- **Name the view.** The same thing drawn as an item icon, in the world, and in a GUI can differ, so
  ask for whichever view the question is about, and say so explicitly.

## Reading it

A 16px sprite is unreadable at full-screen scale, so crop and enlarge before saying anything about it.
There is no ImageMagick or PIL on this machine; use `sips`:

```shell
sips -c <cropH> <cropW> --cropOffset <offsetY> <offsetX> in.png --out out.png
sips -z <newH> <newW> out.png --out out.png    # enlarge, keeps aspect ratio
```

Copy into the scratchpad directory first and work there. Never modify the originals.

## Reporting what is there

Describe the image before interpreting it: what is in which slot, where a marker sits, which side is
empty. Keep that description separate from what it means.

Do not let an expectation fill in pixels. If a detail is ambiguous at the available resolution, say it
is ambiguous and ask for a closer shot rather than picking the reading that fits the code.
