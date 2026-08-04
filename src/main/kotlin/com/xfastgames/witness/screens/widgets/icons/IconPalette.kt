package com.xfastgames.witness.screens.widgets.icons

/**
 * The two tones a rail icon may use.
 *
 * `toggle_image_button.png` is a mid grey in every state a player can read: normal, selected and
 * highlighted all sit around 0.55 luminance, and only disabled is dark. So an icon has exactly two
 * usable directions, well below that grey or well above it, and any mid tone in between dissolves
 * into the button it is drawn on. That is the whole reason these icons are silhouette plus accent
 * rather than shaded drawings.
 */
internal const val ICON_BODY = .25f

/** A band or notch cut through [ICON_BODY], for the one detail a silhouette cannot carry alone. */
internal const val ICON_ACCENT = .85f
