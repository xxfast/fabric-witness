package com.xfastgames.witness.blocks.redstone

import com.xfastgames.witness.items.data.Side

/**
 * The sides a solved frame powers (rules/minecraft/05-puzzle-frame.md, "where the power goes"),
 * in priority: a one-end panel feeds every side; a panel with a choice of ends feeds its one
 * neighbour when, with the side its power came in on left out, it has exactly one; otherwise
 * the used nub's side(s). Pure over panel sides so the priority is tested without Minecraft;
 * the frame block maps sides to world directions.
 *
 * @param exit the used nub's side(s), empty while unsolved
 * @param neighbours the bracket sides holding a frame or a cable
 * @param input the bracket side the frame's power came in on, if it came in on one
 */
fun outputSides(singleEnd: Boolean, exit: Set<Side>, neighbours: Set<Side>, input: Side?): Set<Side> {
    if (singleEnd) return Side.entries.toSet()
    val candidates: Set<Side> = neighbours - setOfNotNull(input)
    return if (candidates.size == 1) candidates else exit
}
