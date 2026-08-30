#!/usr/bin/env bash
# Generates every cable block model and the cable blockstate (rules/minecraft/06-cable.md, "Bends").
# Run from the repo root: bash tools/gen_cable_models.sh
#
# The cable is one square rod of side S that bends in quarter circles of radius R (the block's
# half width, so an arc runs face-centre to face-centre). Two opposite long faces glow (tint 0),
# joins); "shade": false everywhere, because vanilla shades a face
# by its unrotated direction and the arc pieces then jump in brightness beside their neighbours,
# which read as a twist in the top bend (shot 2026-08-30 16:21). A bend carries the glowing pair round with it, so the
# vertical rod above a floor bend glows on the faces the floor's top turned into. Standing pieces
# (bands, their corners and bends, the post) glow on their sides instead: a band out of a frame's
# edge lies in the panel's plane, so its sides face whoever looks at the panel, and the climb
# under it carries those faces straight down (the frame decides the glowing side, 2026-08-30 17:10).
#
# Rotation signs follow JOML (rotationY: +z tips toward +x for a positive angle, confirmed in game
# 2026-08-30; rotationX: +z tips toward -y for a positive angle, derived the same way). SGN_X flips
# every x-axis angle at once if that derivation turns out mirrored.
set -e
S=${S:-2}      # rod side, px
R=8            # bend radius, px
SGN_X=${SGN_X:-1}
H=$(awk -v s=$S 'BEGIN{printf "%.4g", s/2}')
E=$(awk -v r=$R -v h=$H 'BEGIN{printf "%.4g", (r+h)*0.198912}')   # tangent-segment half length at the outer edge
OUT=src/main/resources/assets/witness
TEX='"textures": { "0": "minecraft:block/white_concrete", "particle": "#0" }'

num() { awk "BEGIN{printf \"%.4g\", $1}"; }

# box x1 y1 z1 x2 y2 z2 glow [rotation-json]; glow is ud / ns / ew (the pair that takes tint 0) or sides (all four).
box() {
  local rot=${8:-} f
  echo -n "    { \"from\": [$1, $2, $3], \"to\": [$4, $5, $6], \"shade\": false,"
  [ -n "$rot" ] && echo -n " \"rotation\": $rot,"
  echo -n ' "faces": {'
  local first=1
  # One colour on every face (tint 0): the dark casing on two faces was dropped 2026-08-30 17:55,
  # its pairing rules seamed at joins. $7 (the old glow pair) is accepted and ignored.
  for f in north east south west up down; do
    [ $first = 1 ] || echo -n ","
    first=0
    echo -n " \"$f\": { \"uv\": [0, 0, 1, 1], \"texture\": \"#0\", \"tintindex\": 0 }"
  done
  echo " } }"
}
rot() { echo "{ \"origin\": [$1, $2, $3], \"axis\": \"$4\", \"angle\": $5 }"; }

model() { # model name, then element lines on stdin
  { echo "{"; echo '  "parent": "block/block",'; echo "  $TEX,"; echo '  "elements": ['
    sed '$!s/$/,/'
    echo "  ]"; echo "}"; } > "$OUT/models/block/$1.json"
}

# A quarter circle in the horizontal plane from the north face (8, 0) to the east face (16, 8),
# about the block's north-east corner, with the rod's bottom at y0.
arc_y() {
  local y0=$1 gz=${2:-ud} gx=${3:-ud} y1=$(num "$1+$S") ym=$(num "$1+$H") a px pz
  box $(num "8-$H") $y0 0 $(num "8+$H") $y1 $E $gz
  for a in 157.5 135; do
    px=$(num "16+$R*cos($a*atan2(0,-1)/180)"); pz=$(num "$R*sin($a*atan2(0,-1)/180)")
    box $(num "$px-$H") $y0 $(num "$pz-$E") $(num "$px+$H") $y1 $(num "$pz+$E") $gz "$(rot $px $ym $pz y $(num "180-$a"))"
  done
  a=112.5; px=$(num "16+$R*cos($a*atan2(0,-1)/180)"); pz=$(num "$R*sin($a*atan2(0,-1)/180)")
  box $(num "$px-$E") $y0 $(num "$pz-$H") $(num "$px+$E") $y1 $(num "$pz+$H") $gx "$(rot $px $ym $pz y $(num "90-$a"))"
  box $(num "16-$E") $y0 $(num "8-$H") 16 $y1 $(num "8+$H") $gx
}

# A quarter circle in the plane x = 8 from a rod heading +z at (z 0, y y0) up to a rod heading +y
# at (z 8, y y0 + R): the floor rising into a climb (y0 = 0 rod bottom) or a band rising (y0 = 8 - H).
# The rod centre starts at yc = y0 + H; the arc is about (z 0, yc + R).
arc_up() {
  local y0=$1 gz=${2:-ud} gy=${3:-ns} yc=$(num "$1+$H") a pz py
  box $(num "8-$H") $y0 0 $(num "8+$H") $(num "$y0+$S") $E $gz
  for a in -67.5 -45; do
    pz=$(num "$R*cos($a*atan2(0,-1)/180)"); py=$(num "$yc+$R+$R*sin($a*atan2(0,-1)/180)")
    box $(num "8-$H") $(num "$py-$H") $(num "$pz-$E") $(num "8+$H") $(num "$py+$H") $(num "$pz+$E") $gz "$(rot 8 $py $pz x $(num "$SGN_X*(-90-($a))"))"
  done
  a=-22.5; pz=$(num "$R*cos($a*atan2(0,-1)/180)"); py=$(num "$yc+$R+$R*sin($a*atan2(0,-1)/180)")
  box $(num "8-$H") $(num "$py-$E") $(num "$pz-$H") $(num "8+$H") $(num "$py+$E") $(num "$pz+$H") $gy "$(rot 8 $py $pz x $(num "$SGN_X*(0-($a))"))"
  # The straight tail up to the top face; without it the arc stopped at y = yc + R - E and the rod above began with a cut (shot 2026-08-30 18:10).
  box $(num "8-$H") $(num "$yc+$R-$E") $(num "8-$H") $(num "8+$H") $(num "$yc+$R") $(num "8+$H") $gy
}

# A quarter circle in the plane x = 8 from a rod heading +y at (z 8, y 0) round to a rod heading
# -z at (z 0, y 8): the top of a climb turning into a band. About (z 0, y 0).
arc_down() {
  local a pz py
  box $(num "8-$H") 0 $(num "8-$H") $(num "8+$H") $E $(num "8+$H") ew
  for a in 22.5 45; do
    pz=$(num "$R*cos($a*atan2(0,-1)/180)"); py=$(num "$R*sin($a*atan2(0,-1)/180)")
    box $(num "8-$H") $(num "$py-$E") $(num "$pz-$H") $(num "8+$H") $(num "$py+$E") $(num "$pz+$H") ew "$(rot 8 $py $pz x $(num "$SGN_X*(0-($a))"))"
  done
  a=67.5; pz=$(num "$R*cos($a*atan2(0,-1)/180)"); py=$(num "$R*sin($a*atan2(0,-1)/180)")
  box $(num "8-$H") $(num "$py-$H") $(num "$pz-$E") $(num "8+$H") $(num "$py+$H") $(num "$pz+$E") ew "$(rot 8 $py $pz x $(num "$SGN_X*(90-$a)"))"
  box $(num "8-$H") $(num "8-$H") 0 $(num "8+$H") $(num "8+$H") $E ew
}

C0=$(num "8-$H"); C1=$(num "8+$H"); B0=$(num "8-$H"); B1=$(num "8+$H")
model cable_core        < <(box $C0 0 $C0 $C1 $S $C1 ud)
model cable_post        < <(box $C0 $B0 $C0 $C1 $B1 $C1 sides)
model cable_arm         < <(box $C0 0 0 $C1 $S 8 ud)
model cable_band        < <(box $C0 $B0 0 $C1 $B1 8 ew)
model cable_corner      < <(arc_y 0)
model cable_band_corner < <(arc_y $B0 ew ns)
model cable_riser       < <(box $C0 8 $C0 $C1 16 $C1 ns)
model cable_riser_foot  < <(box $C0 0 $C0 $C1 16 $C1 ns)
model cable_drop        < <(box $C0 0 $C0 $C1 8 $C1 ns)
model cable_foot_bend   < <(arc_up 0; box $C0 $(num "$H+$R-$E") $C0 $C1 16 $C1 ns)
model cable_band_bend_up   < <(arc_up $B0 ew ew)
model cable_band_bend_down < <(arc_down)
{ echo "{"; echo '  "parent": "block/block",'; echo "  $TEX,"
  echo '  "display": { "gui": { "rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8] } },'
  echo '  "elements": ['; box $C0 $B0 0 $C1 $B1 16 ud; echo "  ]"; echo "}"; } > "$OUT/models/block/cable_inventory.json"
rm -f "$OUT/models/block/cable_drop_floor.json"

# ---- blockstate ---------------------------------------------------------------------------------
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
for F in true false; do
  fl=$(q floor $F)
  if [ $F = true ]; then centre=cable_core; arm=cable_arm; corner=cable_corner; up=cable_riser_foot; bendup=cable_foot_bend; down=""; benddown=""
  else centre=cable_post; arm=cable_band; corner=cable_band_corner; up=cable_riser; bendup=cable_band_bend_up; down=cable_drop; benddown=cable_band_bend_down; fi
  # Centre piece: an opposite pair; a lone arm with no single-sided climb; a corner that also climbs; (floor only) nothing at all.
  entries=()
  entries+=("$(when "$fl" "$(q north true)" "$(q south true)")" "$(when "$fl" "$(q east true)" "$(q west true)")")
  for i in 0 1 2 3; do
    d=${dirs[$i]}; others=(); for j in 0 1 2 3; do [ $j = $i ] || others+=("$(q ${dirs[$j]} false)"); done
    # On the floor a drop below is a lip, not a bend, so a lone arm keeps its pad whenever nothing climbs.
    if [ $F = true ]; then entries+=("$(when "$fl" "$(q $d true)" "${others[@]}" "$(q up false)")")
    else entries+=("$(when "$fl" "$(q $d true)" "${others[@]}" "$(q up false)" "$(q down false)")"); fi
    entries+=("$(when "$fl" "$(q $d true)" "${others[@]}" "$(q up true)" "$(q down true)")")
    n=${dirs[$(((i+1)%4))]}; o=${dirs[$(((i+2)%4))]}; p=${dirs[$(((i+3)%4))]}
    entries+=("$(when "$fl" "$(q $d true)" "$(q $n true)" "$(q $o false)" "$(q $p false)" "$(q up true)")" "$(when "$fl" "$(q $d true)" "$(q $n true)" "$(q $o false)" "$(q $p false)" "$(q down true)")")
  done
  [ $F = true ] && entries+=("$(when "$fl" "$(q north false)" "$(q east false)" "$(q south false)" "$(q west false)")")
  part "$(or "${entries[@]}")" $centre ""
  # Arms: shown unless this arm is one side of a corner or the arm of a bend.
  for i in 0 1 2 3; do
    d=${dirs[$i]}; o=${dirs[$(((i+2)%4))]}; l=${dirs[$(((i+1)%4))]}; r=${dirs[$(((i+3)%4))]}
    entries=(
      "$(when "$fl" "$(q $d true)" "$(q $o true)")"
      "$(when "$fl" "$(q $d true)" "$(q $l true)" "$(q $r true)")"
      # On the floor a lone arm over a drop is a lip, not a bend: the arm stays.
      "$(if [ $F = true ]; then when "$fl" "$(q $d true)" "$(q $l false)" "$(q $o false)" "$(q $r false)" "$(q up false)"; else when "$fl" "$(q $d true)" "$(q $l false)" "$(q $o false)" "$(q $r false)" "$(q up false)" "$(q down false)"; fi)"
      "$(when "$fl" "$(q $d true)" "$(q $l false)" "$(q $o false)" "$(q $r false)" "$(q up true)" "$(q down true)")"
      "$(when "$fl" "$(q $d true)" "$(q $l true)" "$(q $r false)" "$(q $o false)" "$(q up true)")"
      "$(when "$fl" "$(q $d true)" "$(q $l true)" "$(q $r false)" "$(q $o false)" "$(q down true)")"
      "$(when "$fl" "$(q $d true)" "$(q $r true)" "$(q $l false)" "$(q $o false)" "$(q up true)")"
      "$(when "$fl" "$(q $d true)" "$(q $r true)" "$(q $l false)" "$(q $o false)" "$(q down true)")"
    )
    part "$(or "${entries[@]}")" $arm "$(y_of ${rot[$i]})"
  done
  # Corners.
  for i in 0 1 2 3; do
    d=${dirs[$i]}; n=${dirs[$(((i+1)%4))]}; o=${dirs[$(((i+2)%4))]}; p=${dirs[$(((i+3)%4))]}
    part "$(when "$fl" "$(q $d true)" "$(q $n true)" "$(q $o false)" "$(q $p false)" "$(q up false)" "$(q down false)")" $corner "$(y_of ${rot[$i]})"
  done
  # Vertical rods: shown when the climb continues through, or there is not exactly one arm to bend into.
  for v in up down; do
    m=$up; b=$bendup; w=down
    if [ $v = down ]; then m=$down; b=$benddown; w=up; fi
    [ -n "$m" ] || continue
    entries=("$(when "$fl" "$(q $v true)" "$(q $w true)")" "$(when "$fl" "$(q $v true)" "$(q north false)" "$(q east false)" "$(q south false)" "$(q west false)")"
             "$(when "$fl" "$(q $v true)" "$(q north true)" "$(q south true)")" "$(when "$fl" "$(q $v true)" "$(q east true)" "$(q west true)")")
    for i in 0 1 2 3; do entries+=("$(when "$fl" "$(q $v true)" "$(q ${dirs[$i]} true)" "$(q ${dirs[$(((i+1)%4))]} true)")"); done
    for wide in x z; do
      wy=""; [ $wide = z ] && wy=', "y": 90'
      part "$(or "${entries[@]}" | sed "s/{ \"floor\"/{ \"wide\": \"$wide\", \"floor\"/g")" $m "$wy"
    done
    # Bends: exactly one arm, this climb only.
    for i in 0 1 2 3; do
      d=${dirs[$i]}; others=(); for j in 0 1 2 3; do [ $j = $i ] || others+=("$(q ${dirs[$j]} false)"); done
      part "$(when "$fl" "$(q $v true)" "$(q $w false)" "$(q $d true)" "${others[@]}")" $b "$(y_of ${rot[$i]})"
    done
  done
done
} > "$PARTS"
{ echo '{'; echo '  "multipart": ['; sed '$ s/,$//' "$PARTS"; echo '  ]'; echo '}'; } > "$OUT/blockstates/cable.json"
rm -f "$PARTS"
echo "generated: S=$S R=$R E=$E"
