package com.heishandashu.moreswitches.config;

/**
 * One user-visible switch.
 *
 * <p>A switch is deliberately just an identity plus a default: what the switch *does* lives in
 * its own module class, and how it is drawn lives in the screen. The settings screen walks a
 * list of these and therefore never needs editing when a feature is added.</p>
 */
public record SwitchDefinition(String id, boolean defaultEnabled) {
}
