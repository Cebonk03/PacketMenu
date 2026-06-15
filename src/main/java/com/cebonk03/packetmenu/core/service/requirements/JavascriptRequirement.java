package com.cebonk03.packetmenu.core.service.requirements;

import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.RequirementContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * A requirement that evaluates an arbitrary JavaScript expression to determine
 * whether it passes.
 *
 * <p>Uses {@link javax.script.ScriptEngineManager} to obtain a scripting engine
 * (Graal.js or Nashorn).  The {@code player} variable is bound to the native
 * Bukkit player object before evaluation.
 *
 * <p>If no JavaScript engine is available the requirement silently fails.
 */
public final class JavascriptRequirement implements Requirement {

    private static final ScriptEngineManager ENGINE_MANAGER = new ScriptEngineManager();

    private final String script;

    /**
     * Creates a new {@code JavascriptRequirement}.
     *
     * @param script the JavaScript expression to evaluate
     */
    public JavascriptRequirement(final String script) {
        this.script = script;
    }

    @Override
    public boolean test(final RequirementContext context) {
        ScriptEngine engine = ENGINE_MANAGER.getEngineByName("graal.js");
        if (engine == null) {
            engine = ENGINE_MANAGER.getEngineByName("nashorn");
        }
        if (engine == null) {
            return false;
        }

        engine.put("player", context.viewer().nativePlayer());
        try {
            final Object result = engine.eval(script);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            // Non-boolean results are treated as truthy
            return result != null;
        } catch (final ScriptException e) {
            return false;
        }
    }
}
