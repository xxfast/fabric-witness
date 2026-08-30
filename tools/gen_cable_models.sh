#!/usr/bin/env bash
# Generates every cable block model and the cable blockstate (rules/minecraft/06-cable.md, "Bends").
# Run from the repo root: bash tools/gen_cable_models.sh
#
# The cable is a flat ribbon, W wide and T thick, that bends in quarter circles of radius R (the
# block's half width, so an arc runs face-centre to face-centre) built from five 22.5°-stepped
# elements, the most JSON rotation allows. A ribbon bends two ways: over its face (the bend axis
# is its width axis) or round its edge (the width axis turns with the bend); which one a joint
# uses is the block's `wide` state, decided by CableRibbon.kt. Horizontal pieces at mid height
# come in a flat set (wide x|z) and a standing set (wide y, suffix _s); vertical rods are wide
# across x, or rotated 90° for wide z. Floor pieces are always flat.
#
# Every face takes tint 0 and "shade": false (the casing on two faces was dropped 2026-08-30, and
# vanilla shades a face by its unrotated direction, which made arc pieces jump in brightness).
#
# Rotation signs follow JOML (rotationY: +z tips toward +x for a positive angle, confirmed in game
# 2026-08-30; rotationX: +z tips toward -y for a positive angle, also confirmed 16:21).
set -e
W=${W:-3}      # ribbon width, px
T=${T:-1}      # ribbon thickness, px
R=8            # bend radius, px
HW=$(awk -v w=$W 'BEGIN{printf "%.4g", w/2}')
HT=$(awk -v t=$T 'BEGIN{printf "%.4g", t/2}')
OUT=src/main/resources/assets/witness
TEX='"textures": { "0": "minecraft:block/white_concrete", "particle": "#0" }'

num() { awk "BEGIN{printf \"%.4g\", $1}"; }
# Tangent-segment half length so the arc's outer edge closes: (R + outer half extent) * tan(11.25°).
tangent() { num "($R+$1)*0.198912"; }

# cub cx cy cz hx hy hz [rotation-json]: a cuboid by centre and half extents, one colour all round.
cub() {
  local rot=${7:-} f first=1
  echo -n "    { \"from\": [$(num "$1-$4"), $(num "$2-$5"), $(num "$3-$6")], \"to\": [$(num "$1+$4"), $(num "$2+$5"), $(num "$3+$6")], \"shade\": false,"
  [ -n "$rot" ] && echo -n " \"rotation\": $rot,"
  echo -n ' "faces": {'
  for f in north east south west up down; do
    [ $first = 1 ] || echo -n ","
    first=0
    echo -n " \"$f\": { \"uv\": [0, 0, 1, 1], \"texture\": \"#0\", \"tintindex\": 0 }"
  done
  echo " } }"
}
rot() { echo "{ \"origin\": [$1, $2, $3], \"axis\": \"$4\", \"angle\": $5 }"; }
model() { # name; element lines on stdin
  { echo "{"; echo '  "parent": "block/block",'; echo "  $TEX,"; echo '  "elements": ['
    sed '$!s/$/,/'
    echo "  ]"; echo "}"; } > "$OUT/models/block/$1.json"
}
cs() { awk -v a=$1 'BEGIN{printf "%.4g", cos(a*atan2(0,-1)/180)}'; }
sn() { awk -v a=$1 'BEGIN{printf "%.4g", sin(a*atan2(0,-1)/180)}'; }

# ---- arcs ----------------------------------------------------------------------------------------
# Each takes the ribbon's half extents along the bend's radial direction (hr) and across the
# bend plane (ha): flat in the horizontal plane is hr=HW ha=HT (thin in y), standing is hr=HT ha=HW.

# Horizontal plane, from the north face (8, 0) to the east face (16, 8), about the block's
# north-east corner, ribbon centred at height yc.
arc_h() {
  local yc=$1 hr=$2 ha=$3 E=$(tangent $2) a px pz
  cub 8 $yc $(num "$E/2") $hr $ha $(num "$E/2")
  for a in 157.5 135; do
    px=$(num "16+$R*$(cs $a)"); pz=$(num "$R*$(sn $a)")
    cub $px $yc $pz $hr $ha $E "$(rot $px $yc $pz y $(num "180-$a"))"
  done
  a=112.5; px=$(num "16+$R*$(cs $a)"); pz=$(num "$R*$(sn $a)")
  cub $px $yc $pz $E $ha $hr "$(rot $px $yc $pz y $(num "90-$a"))"
  cub $(num "16-$E/2") $yc 8 $(num "$E/2") $ha $hr
}

# Plane x = 8, from a piece heading +z at (z 0, y yc) up to a piece heading +y at (z 8, y yc + R),
# about (z 0, y yc + R). Radial half extent hr, across-plane (x) half extent ha.
arc_up() {
  local yc=$1 hr=$2 ha=$3 E=$(tangent $2) a pz py
  cub 8 $yc $(num "$E/2") $ha $hr $(num "$E/2")
  for a in -67.5 -45; do
    pz=$(num "$R*$(cs $a)"); py=$(num "$yc+$R+$R*$(sn $a)")
    cub 8 $py $pz $ha $hr $E "$(rot 8 $py $pz x $(num "-90-($a)"))"
  done
  a=-22.5; pz=$(num "$R*$(cs $a)"); py=$(num "$yc+$R+$R*$(sn $a)")
  cub 8 $py $pz $ha $E $hr "$(rot 8 $py $pz x $(num "0-($a)"))"
  # The straight tail to the top face; without it the arc stopped short and the rod above began with a cut (2026-08-30 18:10).
  cub 8 $(num "$yc+$R-$E/2") 8 $ha $(num "$E/2") $hr
}

# Plane x = 8, from a piece heading +y at (z 8, y 0) round to a piece heading -z at (z 0, y 8), about (z 0, y 0).
arc_down() {
  local hr=$1 ha=$2 E=$(tangent $1) a pz py
  cub 8 $(num "$E/2") 8 $ha $(num "$E/2") $hr
  for a in 22.5 45; do
    pz=$(num "$R*$(cs $a)"); py=$(num "$R*$(sn $a)")
    cub 8 $py $pz $ha $E $hr "$(rot 8 $py $pz x $(num "0-($a)"))"
  done
  a=67.5; pz=$(num "$R*$(cs $a)"); py=$(num "$R*$(sn $a)")
  cub 8 $py $pz $ha $hr $E "$(rot 8 $py $pz x $(num "90-$a"))"
  cub 8 8 $(num "$E/2") $ha $hr $(num "$E/2")
}

# Plane x = 8, from a piece heading +z at (z 0, y yc) over the edge and down to a piece heading -y
# at (z 8, y yc - R), about (z 0, y yc - R). A floor lip: most of the arc hangs below the block,
# into the rod under it, which skips its top half (`under_lip`) to make room.
arc_over() {
  local yc=$1 hr=$2 ha=$3 E=$(tangent $2) a pz py
  cub 8 $yc $(num "$E/2") $ha $hr $(num "$E/2")
  for a in 22.5 45; do
    pz=$(num "$R*$(sn $a)"); py=$(num "$yc-$R+$R*$(cs $a)")
    cub 8 $py $pz $ha $hr $E "$(rot 8 $py $pz x $a)"
  done
  a=67.5; pz=$(num "$R*$(sn $a)"); py=$(num "$yc-$R+$R*$(cs $a)")
  cub 8 $py $pz $ha $E $hr "$(rot 8 $py $pz x $(num "$a-90"))"
  # The tail reaches a pixel past the arc's end so it overlaps the rod below (a half-pixel notch showed at 20:10).
  cub 8 $(num "$yc-$R+($E-1)/2") 8 $ha $(num "($E+1)/2") $hr
}

# ---- models --------------------------------------------------------------------------------------
YF=$HT   # centre height of a floor ribbon
YB=8     # centre height of a mid-height band
# Floor: flat. Vertical rods: wide across x (hx=HW, hz=HT); a floor foot rises face-first, so its rod is wide across x too.
model cable_core       < <(cub 8 $YF 8 $HW $HT $HW)
model cable_arm        < <(cub 8 $YF 4 $HW $HT 4)
model cable_corner     < <(arc_h $YF $HW $HT)
model cable_riser_foot < <(cub 8 8 8 $HW 8 $HT)
# Under a lip the floor rod only reaches the lip's hanging arc (a gap showed at 20:35).
model cable_riser_foot_short < <(cub 8 4.5 8 $HW 4.5 $HT)
model cable_foot_bend  < <(arc_up $YF $HT $HW; cub 8 $(num "(16+$YF+$R)/2") 8 $HW $(num "(16-$YF-$R)/2") $HT)
model cable_foot_bend_short < <(arc_up $YF $HT $HW)
model cable_lip_bend   < <(arc_over $YF $HT $HW)
model cable_riser      < <(cub 8 12 8 $HW 4 $HT)
model cable_drop       < <(cub 8 4 8 $HW 4 $HT)
# Mid-height horizontals, flat (thin in y) ...
model cable_post           < <(cub 8 $YB 8 $HW $HT $HW)
model cable_band           < <(cub 8 $YB 4 $HW $HT 4)
model cable_band_corner    < <(arc_h $YB $HW $HT)
model cable_band_bend_up   < <(arc_up $YB $HT $HW)
model cable_band_bend_down < <(arc_down $HT $HW)
# ... and standing on edge (thin across the band, tall in y).
model cable_post_s           < <(cub 8 $YB 8 $HT $HW $HT)
model cable_band_s           < <(cub 8 $YB 4 $HT $HW 4)
model cable_band_corner_s    < <(arc_h $YB $HT $HW)
model cable_band_bend_up_s   < <(arc_up $YB $HW $HT)
model cable_band_bend_down_s < <(arc_down $HW $HT)
{ echo "{"; echo '  "parent": "block/block",'; echo "  $TEX,"
  echo '  "display": { "gui": { "rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8] } },'
  echo '  "elements": ['; cub 8 $YB 8 $HW $HT 8; echo "  ]"; echo "}"; } > "$OUT/models/block/cable_inventory.json"
rm -f "$OUT/models/block/cable_drop_floor.json"

# ---- blockstate ----------------------------------------------------------------------------------
# By horizontal arms: a corner is exactly two perpendicular arms; a bend is exactly one arm with a
# climb on one side only. Each replaces the centre piece, the arm(s) and the vertical rod it bends.
dirs=(north east south west); rot=(0 90 180 270)
y_of() { [ "$1" = 0 ] || printf ', "y": %s' "$1"; }
when() { local IFS=,; echo "{ ${*} }"; }
part() { printf '    { "when": %s, "apply": { "model": "witness:block/%s"%s } },\n' "$1" "$2" "$3"; }
or() { local out="" e; for e in "$@"; do out+="${out:+, }$e"; done; echo "{ \"OR\": [ $out ] }"; }
q() { printf '"%s": "%s"' "$1" "$2"; }

PARTS=$(mktemp)
{
# Three sets: floor pieces (always flat), mid-height flat (wide x or z), mid-height standing (wide y).
for set in "true::" "false::x|z" "false:_s:y"; do
  IFS=':' read -r F SUF WIDEV <<< "$set"
  fl=$(q floor $F)
  cond=("$fl"); [ -n "$WIDEV" ] && cond+=("$(q wide "$WIDEV")")
  if [ $F = true ]; then centre=cable_core; arm=cable_arm; corner=cable_corner; bendup=cable_foot_bend; benddown=cable_lip_bend
  else centre=cable_post$SUF; arm=cable_band$SUF; corner=cable_band_corner$SUF; bendup=cable_band_bend_up$SUF; benddown=cable_band_bend_down$SUF; fi
  # Centre piece: an opposite pair; a lone arm with no single-sided climb; a corner that also climbs; (floor only) nothing at all.
  entries=()
  entries+=("$(when "${cond[@]}" "$(q north true)" "$(q south true)")" "$(when "${cond[@]}" "$(q east true)" "$(q west true)")")
  for i in 0 1 2 3; do
    d=${dirs[$i]}; others=(); for j in 0 1 2 3; do [ $j = $i ] || others+=("$(q ${dirs[$j]} false)"); done
    entries+=("$(when "${cond[@]}" "$(q $d true)" "${others[@]}" "$(q up false)" "$(q down false)")")
    entries+=("$(when "${cond[@]}" "$(q $d true)" "${others[@]}" "$(q up true)" "$(q down true)")")
    n=${dirs[$(((i+1)%4))]}; o=${dirs[$(((i+2)%4))]}; p=${dirs[$(((i+3)%4))]}
    entries+=("$(when "${cond[@]}" "$(q $d true)" "$(q $n true)" "$(q $o false)" "$(q $p false)" "$(q up true)")" "$(when "${cond[@]}" "$(q $d true)" "$(q $n true)" "$(q $o false)" "$(q $p false)" "$(q down true)")")
  done
  [ $F = true ] && entries+=("$(when "${cond[@]}" "$(q north false)" "$(q east false)" "$(q south false)" "$(q west false)")")
  part "$(or "${entries[@]}")" $centre ""
  # Arms: shown unless this arm is one side of a corner or the arm of a bend.
  for i in 0 1 2 3; do
    d=${dirs[$i]}; o=${dirs[$(((i+2)%4))]}; l=${dirs[$(((i+1)%4))]}; r=${dirs[$(((i+3)%4))]}
    entries=(
      "$(when "${cond[@]}" "$(q $d true)" "$(q $o true)")"
      "$(when "${cond[@]}" "$(q $d true)" "$(q $l true)" "$(q $r true)")"
      "$(when "${cond[@]}" "$(q $d true)" "$(q $l false)" "$(q $o false)" "$(q $r false)" "$(q up false)" "$(q down false)")"
      "$(when "${cond[@]}" "$(q $d true)" "$(q $l false)" "$(q $o false)" "$(q $r false)" "$(q up true)" "$(q down true)")"
      "$(when "${cond[@]}" "$(q $d true)" "$(q $l true)" "$(q $r false)" "$(q $o false)" "$(q up true)")"
      "$(when "${cond[@]}" "$(q $d true)" "$(q $l true)" "$(q $r false)" "$(q $o false)" "$(q down true)")"
      "$(when "${cond[@]}" "$(q $d true)" "$(q $r true)" "$(q $l false)" "$(q $o false)" "$(q up true)")"
      "$(when "${cond[@]}" "$(q $d true)" "$(q $r true)" "$(q $l false)" "$(q $o false)" "$(q down true)")"
    )
    part "$(or "${entries[@]}")" $arm "$(y_of ${rot[$i]})"
  done
  # Corners.
  for i in 0 1 2 3; do
    d=${dirs[$i]}; n=${dirs[$(((i+1)%4))]}; o=${dirs[$(((i+2)%4))]}; p=${dirs[$(((i+3)%4))]}
    part "$(when "${cond[@]}" "$(q $d true)" "$(q $n true)" "$(q $o false)" "$(q $p false)" "$(q up false)" "$(q down false)")" $corner "$(y_of ${rot[$i]})"
  done
  # Bends: one per arm whenever the piece climbs on one side only, so a rod out of a straight run
  # forks smoothly into both directions (a lone arm keeps its bend and drops its arm and centre).
  for v in up down; do
    b=$bendup; w=down
    if [ $v = down ]; then b=$benddown; w=up; fi
    [ -n "$b" ] || continue
    for i in 0 1 2 3; do
      d=${dirs[$i]}; o=${dirs[$(((i+2)%4))]}; l=${dirs[$(((i+1)%4))]}; r=${dirs[$(((i+3)%4))]}
      # Only with one or two arms: three or four arcs piling onto the pad read as a tangle (20:22),
      # so a junction of three or more takes a plain rod through its centre instead.
      # Exactly one arm: with two or more, a plain rod through the pad reads better than arcs
      # (four arcs were a tangle at 20:22, a forked pair still too busy at 20:31).
      few() { # extra conditions...
        when "${cond[@]}" "$@" "$(q $v true)" "$(q $w false)" "$(q $d true)" "$(q $o false)" "$(q $l false)" "$(q $r false)"
      }
      if [ $F = true ] && [ $v = up ]; then
        # A foot under a lip has no tail: the lip's arc hangs down over it.
        part "$(few "$(q under_lip false)")" $b "$(y_of ${rot[$i]})"
        part "$(few "$(q under_lip true)")" ${b}_short "$(y_of ${rot[$i]})"
      else
        part "$(few)" $b "$(y_of ${rot[$i]})"
      fi
    done
  done
done
# Vertical rods, by floor: shown when the climb continues through, or there is no arm to bend into.
for F in true false; do
  fl=$(q floor $F)
  for v in up down; do
    w=down; m=cable_riser; [ $F = true ] && m=cable_riser_foot
    if [ $v = down ]; then w=up; m=cable_drop; [ $F = true ] && continue; fi
    # Through, no arm at all, or two or more arms (an opposite pair or a perpendicular pair covers every such case).
    entries=("$(when "$fl" "$(q $v true)" "$(q $w true)")" "$(when "$fl" "$(q $v true)" "$(q north false)" "$(q east false)" "$(q south false)" "$(q west false)")"
             "$(when "$fl" "$(q $v true)" "$(q north true)" "$(q south true)")" "$(when "$fl" "$(q $v true)" "$(q east true)" "$(q west true)")")
    for i in 0 1 2 3; do entries+=("$(when "$fl" "$(q $v true)" "$(q ${dirs[$i]} true)" "$(q ${dirs[$(((i+1)%4))]} true)")"); done
    # A rod under a floor lip skips its top half: the lip's bend hangs down into it.
    lip=""; [ $v = up ] && lip='"under_lip": "false", '
    part "$(or "${entries[@]}" | sed "s/{ \"floor\"/{ $lip\"wide\": \"x|y\", \"floor\"/g")" $m ""
    part "$(or "${entries[@]}" | sed "s/{ \"floor\"/{ $lip\"wide\": \"z\", \"floor\"/g")" $m ', "y": 90'
    if [ $F = true ] && [ $v = up ]; then
      part "$(or "${entries[@]}" | sed "s/{ \"floor\"/{ \"under_lip\": \"true\", \"wide\": \"x|y\", \"floor\"/g")" cable_riser_foot_short ""
      part "$(or "${entries[@]}" | sed "s/{ \"floor\"/{ \"under_lip\": \"true\", \"wide\": \"z\", \"floor\"/g")" cable_riser_foot_short ', "y": 90'
    fi
  done
done
} > "$PARTS"
{ echo '{'; echo '  "multipart": ['; sed '$ s/,$//' "$PARTS"; echo '  ]'; echo '}'; } > "$OUT/blockstates/cable.json"
rm -f "$PARTS"
echo "generated: W=$W T=$T R=$R"
