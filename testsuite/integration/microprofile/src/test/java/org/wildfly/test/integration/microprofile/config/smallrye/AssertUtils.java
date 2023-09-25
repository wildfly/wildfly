/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class AssertUtils {

    public static void assertTextContainsProperty(String text, String propName, Object propValue) {
        assertTextContainsProperty(text, propName, propValue, true);
    }

    public static void assertTextContainsProperty(String text, String propName, Object propValue, boolean exactMatch) {
        if (exactMatch) {
            // Include also newline at the end of the line to assure that whole value is checked.
            assertTrue("String '" + propName + " = " + propValue + "' not found in the following output:\n" + text,
                    text.contains(propName + " = " + propValue + "\n"));
        } else {
            // Find the line starting with "${propName} ="
            List<String> matchingLines = Arrays.stream(text.split("\n"))
                    .filter(line -> line.startsWith(propName + " ="))
                    .collect(Collectors.toList());
            assertEquals(1, matchingLines.size());
            assertTrue("Text " + propValue + "not found for property " + propName + " in " + text,
                    matchingLines.get(0).contains(propValue.toString()));

        }
    }
}
