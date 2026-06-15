package com.cebonk03.packetmenu.core.port;

import com.cebonk03.packetmenu.core.domain.MenuAction;

/**
 * Port for parsing raw action strings into domain {@link MenuAction} instances.
 *
 * <p>Raw action strings come from configuration files or in-game commands and
 * describe the behaviour to execute when a player interacts with a menu slot.
 */
public interface ActionParser {

    /**
     * Parses a raw action string into a {@link MenuAction}.
     *
     * @param raw the raw action string (e.g. {@code "[player] command say hi"})
     * @return the parsed {@link MenuAction}
     */
    MenuAction parse(String raw);
}
