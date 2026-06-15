package com.cebonk03.packetmenu.core.domain;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A template for an animated inventory slot that cycles through multiple frames.
 *
 * <p>Each frame specifies the item, an optional visibility requirement, and
 * click-type-specific actions. The animation advances by {@link #ticksPerFrame()}
 * server ticks per frame and optionally loops.
 *
 * @param frames       the ordered list of animation frames
 * @param ticksPerFrame how many server ticks each frame is displayed for
 * @param loop         whether the animation loops back to the start after the last frame
 */
@NullMarked
public record AnimatedSlotTemplate(
    List<AnimationFrame> frames,
    int ticksPerFrame,
    boolean loop
) {

    /**
     * Compact constructor that defensively copies the frame list.
     */
    public AnimatedSlotTemplate {
        frames = List.copyOf(frames);
    }

    /**
     * Returns the frame that should be displayed at the given server tick.
     *
     * @param tick the current server tick
     * @return the active animation frame, or {@code null} if the frame list is empty
     */
    @Nullable
    public AnimationFrame currentFrame(final long tick) {
        if (frames.isEmpty()) {
            return null;
        }
        final int index = (int) ((tick / ticksPerFrame) % frames.size());
        return frames.get(index);
    }

    /**
     * A single frame within an animated slot sequence.
     *
     * @param item             the item snapshot to display during this frame
     * @param viewRequirement  optional predicate controlling visibility during this frame
     * @param actions          actions mapped by click type for this frame
     */
    public record AnimationFrame(
        ItemStackSnapshot item,
        @Nullable ViewRequirement viewRequirement,
        Map<ClickType, List<MenuAction>> actions
    ) {

        /**
         * Compact constructor that defensively copies the actions map.
         */
        public AnimationFrame {
            actions = Map.copyOf(actions);
        }
    }
}
