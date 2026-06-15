package com.cebonk03.packetmenu.core.service.requirements;

import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.RequirementContext;

/**
 * A requirement that passes when the viewer has a specific permission node.
 *
 * <p>Delegates to {@link com.cebonk03.packetmenu.core.port.PlayerHandle#hasPermission(String)}.
 */
public final class HasPermissionRequirement implements Requirement {

    private final String permission;

    /**
     * Creates a new {@code HasPermissionRequirement}.
     *
     * @param permission the permission node to check
     */
    public HasPermissionRequirement(final String permission) {
        this.permission = permission;
    }

    @Override
    public boolean test(final RequirementContext context) {
        return context.viewer().hasPermission(permission);
    }
}
