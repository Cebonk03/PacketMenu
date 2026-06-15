package com.cebonk03.packetmenu.core.port;

import com.cebonk03.packetmenu.core.domain.MenuTemplate;
import java.nio.file.Path;
import java.util.Map;

/**
 * Port for loading menu configurations from the file system.
 *
 * <p>Implementations parse structured menu files (e.g. YAML, JSON, HOCON)
 * into domain {@link MenuTemplate} objects for use by the menu service.
 */
public interface MenuLoader {

    /**
     * Loads a single menu template from the given file path.
     *
     * @param path the file path to the menu definition
     * @return the parsed {@link MenuTemplate}
     */
    MenuTemplate load(Path path);

    /**
     * Loads all menu templates found in the given directory.
     *
     * <p>The returned map key is the menu identifier (e.g. file name stem).
     *
     * @param directory the directory containing menu definition files
     * @return a map of menu identifiers to their {@link MenuTemplate}
     */
    Map<String, MenuTemplate> loadAll(Path directory);
}
