/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.cli;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

/**
 *
 * @author Alexey Loubyansky
 */
public class CLIPatchInfoUtil {

    private static final String PATCH_ID = "Patch ID";
    private static final String TYPE = "Type";
    private static final String IDENTITY_NAME = "Identity name";
    private static final String IDENTITY_VERSION = "Identity version";
    private static final String DESCR = "Description";

    private static final String CP = "cumulative";
    private static final String ONE_OFF = "one-off";

    public static void assertPatchInfo(byte[] info, String patchId, boolean oneOff, String targetName, String targetVersion,
            String description) {
        final ByteArrayInputStream bis = new ByteArrayInputStream(info);
        final InputStreamReader reader = new InputStreamReader(bis);
        final BufferedReader buf = new BufferedReader(reader);

        final Map<String,String> actual = parseTable(buf);

        final Map<String,String> expected = new HashMap<String,String>();
        expected.put(PATCH_ID, patchId);
        expected.put(TYPE, oneOff ? ONE_OFF : CP);
        expected.put(IDENTITY_NAME, targetName);
        expected.put(IDENTITY_VERSION, targetVersion);
        expected.put(DESCR, description);

        Assert.assertEquals(expected, actual);
    }

    public static void assertPatchInfo(byte[] info, String patchId, boolean oneOff, String targetName, String targetVersion,
            String description, List<Map<String,String>> elements) {
        final ByteArrayInputStream bis = new ByteArrayInputStream(info);
        final InputStreamReader reader = new InputStreamReader(bis);
        final BufferedReader buf = new BufferedReader(reader);

        final Map<String,String> expected = new HashMap<String,String>();
        expected.put(PATCH_ID, patchId);
        expected.put(TYPE, oneOff ? ONE_OFF : CP);
        expected.put(IDENTITY_NAME, targetName);
        expected.put(IDENTITY_VERSION, targetVersion);
        expected.put(DESCR, description);

        Map<String, String> actual = parseTable(buf);
        Assert.assertEquals(expected, actual);

        try {
            if(buf.ready()) {
                String readLine = buf.readLine();
                if(!"ELEMENTS".equals(readLine)) {
                    Assert.fail("Expected 'ELEMENTS' but was '" + readLine + "'");
                }
                if(!buf.ready()) {
                    Assert.fail("Expected an empty line");
                }
                readLine = buf.readLine();
                if(readLine == null || !readLine.isEmpty()) {
                    Assert.fail("Expected an empty line but received '" + readLine + "'");
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read the buffer", e);
        }

        for(Map<String,String> e : elements) {
            try {
                if(!buf.ready()) {
                    Assert.fail("No more output");
                }
            } catch (IOException e1) {
                throw new IllegalStateException("Failed to check the state of the reader", e1);
            }

            actual = parseTable(buf);
            Assert.assertEquals(e, actual);
        }

        try {
            if(buf.ready()) {
                final StringBuilder str = new StringBuilder();
                String line;
                while((line = buf.readLine()) != null) {
                    str.append(line).append("\n");
                }
                Assert.fail("The output contained more info: " + str.toString());
            }
        } catch (IOException e1) {
            throw new IllegalStateException("Failed to read the reader", e1);
        }
    }

    private static Map<String, String> parseTable(final BufferedReader buf) {
        final Map<String,String> actual = new HashMap<String,String>();
        try {
            String line = null;
            while((line = buf.readLine()) != null && !line.isEmpty()) {
                final int colon = line.indexOf(':');
                if(colon < 0) {
                    Assert.fail("Failed to locate ':' in '" + line + "'");
                }
                if(colon == line.length() - 1) {
                    Assert.fail("The line appears to end on ':'");
                }
                actual.put(line.substring(0, colon), line.substring(colon + 1).trim());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read info", e);
        }
        return actual;
    }
}
