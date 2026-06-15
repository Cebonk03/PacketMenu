package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;

/**
 * Parses a JSON-serialized Adventure {@link Component} from a string and
 * broadcasts the resulting component to all online players.
 *
 * <p>If the JSON string is malformed the action returns a {@link ActionResult.Failure}
 * and does not broadcast.
 */
@NullMarked
public final class JsonBroadcastAction implements MenuAction {

    private final String json;
    private final SchedulerPort scheduler;

    /**
     * Creates a new JSON broadcast action.
     *
     * @param json      the JSON-serialized component to broadcast
     * @param scheduler the scheduler on which to run the broadcast
     */
    public JsonBroadcastAction(final String json, final SchedulerPort scheduler) {
        this.json = json;
        this.scheduler = scheduler;
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        final Component component;
        try {
            component = GsonComponentSerializer.gson().deserialize(json);
        } catch (final Exception e) {
            return new ActionResult.Failure(
                    "Failed to deserialize broadcast component: " + e.getMessage());
        }

        scheduler.runOnGlobal(() -> Bukkit.broadcast(component));
        return new ActionResult.Success();
    }
}
