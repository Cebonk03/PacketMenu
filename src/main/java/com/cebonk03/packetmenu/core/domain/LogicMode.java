package com.cebonk03.packetmenu.core.domain;

/**
 * Logic mode for combining multiple requirements in a {@link RequirementSet}.
 *
 * <p>{@link #AND} requires <em>all</em> individual requirements to pass;
 * {@link #OR} requires <em>at least one</em> to pass.
 */
public enum LogicMode {
    AND,
    OR
}
