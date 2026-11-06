package com.heishandashu.moreswitches.carpet;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rewriting a Carpet configuration file.
 *
 * <p>The point of these cases is that a player's other settings and comments must survive: this
 * mod owns a single rule line and nothing else.</p>
 */
class CarpetConfigTest {

    @Test
    void theRuleIsAppendedWhenTheFileIsEmpty() {
        assertEquals(List.of("commandPlayer true"), CarpetConfig.withRule(List.of(), "true"));
    }

    @Test
    void commentsAndOtherRulesAreKept() {
        List<String> input = List.of(
                "# Carpet config",
                "",
                "someOtherRule false",
                "commandPlayer ops",
                "anotherRule 3");

        List<String> out = CarpetConfig.withRule(input, "true");

        assertEquals(List.of(
                "# Carpet config",
                "",
                "someOtherRule false",
                "commandPlayer true",
                "anotherRule 3"), out);
    }

    @Test
    void theRuleIsAddedWhenTheFileHasOtherSettingsOnly() {
        List<String> out = CarpetConfig.withRule(List.of("otherRule true"), "true");
        assertEquals(List.of("otherRule true", "commandPlayer true"), out);
    }

    @Test
    void duplicateRuleLinesCollapseToOne() {
        List<String> out = CarpetConfig.withRule(
                List.of("commandPlayer ops", "commandPlayer false"), "true");
        assertEquals(List.of("commandPlayer true"), out,
                "the last line wins in Carpet, so duplicates must not be left behind");
    }

    @Test
    void aCommentMentioningTheRuleIsNotTreatedAsTheRule() {
        assertFalse(CarpetConfig.isRuleLine("# commandPlayer ops"));
        assertFalse(CarpetConfig.isRuleLine(""));
        assertFalse(CarpetConfig.isRuleLine("commandPlayerOther true"));
        assertTrue(CarpetConfig.isRuleLine("commandPlayer ops"));
        assertTrue(CarpetConfig.isRuleLine("  commandPlayer   ops  "));
    }

    @Test
    void theCurrentValueIsReadBack() {
        assertEquals("ops", CarpetConfig.currentValue(List.of("commandPlayer ops")));
        assertEquals("true", CarpetConfig.currentValue(List.of("# c", "commandPlayer   true")));
        assertNull(CarpetConfig.currentValue(List.of("otherRule true")),
                "an absent rule has no value");
    }

    @Test
    void writingTheSameValueChangesNothing() {
        List<String> input = List.of("commandPlayer true");
        assertEquals(input, CarpetConfig.withRule(input, "true"));
    }
}
