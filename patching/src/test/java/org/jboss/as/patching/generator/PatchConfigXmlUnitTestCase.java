/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.patching.metadata.Patch;
import org.junit.Test;

/**
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class PatchConfigXmlUnitTestCase {

    @Test
    public void testCumulativeGenerated() throws Exception {

        final InputStream is = getResource("test-config01.xml");
        final PatchConfig patchConfig = PatchConfigXml.parse(is);
        // Patch
        assertNotNull(patchConfig);
        assertEquals("patch-12345", patchConfig.getPatchId());
        assertEquals("patch description", patchConfig.getDescription());
        assertNotNull(patchConfig.getPatchType());
        assertEquals(Patch.PatchType.CUMULATIVE, patchConfig.getPatchType());
        assertTrue(patchConfig.isGenerateByDiff());
        assertEquals("2.3.4", patchConfig.getResultingVersion());

        validateAppliesTo(patchConfig, "1.2.3", "1.2.4");

        validateInRuntimeUse(patchConfig);
    }

    @Test
    public void testOneOffGenerated() throws Exception {

        final InputStream is = getResource("test-config02.xml");
        final PatchConfig patchConfig = PatchConfigXml.parse(is);
        // Patch
        assertNotNull(patchConfig);
        assertEquals("patch-12345", patchConfig.getPatchId());
        assertEquals("patch description", patchConfig.getDescription());
        assertNotNull(patchConfig.getPatchType());
        assertEquals(Patch.PatchType.ONE_OFF, patchConfig.getPatchType());
        assertTrue(patchConfig.isGenerateByDiff());
        assertNull(patchConfig.getResultingVersion());

        validateAppliesTo(patchConfig, "1.2.3", "1.2.4");

        validateInRuntimeUse(patchConfig);
    }

    private static InputStream getResource(String name) throws IOException {
        final URL resource = PatchConfigXmlUnitTestCase.class.getClassLoader().getResource(name);
        assertNotNull(name, resource);
        return resource.openStream();
    }

    private static void validateAppliesTo(final PatchConfig patchConfig, String... expected) {
        Set<String> expectedAppliesTo = new HashSet<String>(Arrays.asList(expected));
        Set<String> actualAppliesTo = new HashSet<String>(patchConfig.getAppliesTo());

        assertEquals(expectedAppliesTo, actualAppliesTo);
    }

    private static void validateInRuntimeUse(final PatchConfig patchConfig) {
        Set<String> validInUse = new HashSet<String>(Arrays.asList("test", "test/file", "test/file/file1"));
        for (DistributionContentItem item : patchConfig.getInRuntimeUseItems()) {
            String path = item.getPath();
            assertTrue(path + " is valid", validInUse.remove(path));
        }
        assertEquals("Expected in-runtime-use item not found", Collections.emptySet(), validInUse);
    }

}
