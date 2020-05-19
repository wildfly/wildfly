/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
