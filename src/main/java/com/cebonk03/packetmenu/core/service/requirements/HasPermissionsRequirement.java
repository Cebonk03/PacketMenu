package com.cebonk03.packetmenu.core.service.requirements;

import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.RequirementContext;
import java.util.List;

/**
 * A requirement that passes when the viewer has at least {@code minimumCount}
 * of the listed permissions.
 */
public final class HasPermissionsRequirement implements Requirement {

    private final List<String> permissions;
    private final int minimumCount;

    /**
     * Creates a new {@code HasPermissionsRequirement}.
     *
     * @param permissions  the permission nodes to check
     * @param minimumCount the minimum number of permissions the player must have
     */
    public HasPermissionsRequirement(final List<String> permissions, final int minimumCount) {
        this.permissions = List.copyOf(permissions);
        this.minimumCount = minimumCount;
    }

    @Override
    public boolean test(final RequirementContext context) {
        int count = 0;
        for (final String perm : permissions) {
            if (context.viewer().hasPermission(perm)) {
                count++;
                if (count >= minimumCount) {
                    return true;
                }
            }
        }
        return count >= minimumCount;
    }
}
