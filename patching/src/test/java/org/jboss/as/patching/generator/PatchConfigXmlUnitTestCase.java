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
import static org.junit.Assert.assertFalse;
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

        validateAppliesTo(patchConfig, "1.2.3");

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

        validateAppliesTo(patchConfig, "1.2.3");

        validateInRuntimeUse(patchConfig);
    }

    @Test
    public void testCumulativeSpecified() throws Exception {

        specifiedContentTest("test-config03.xml");
    }

    @Test
    public void testOneOffSpecified() throws Exception {

        specifiedContentTest("test-config04.xml");
    }

    private void specifiedContentTest(String configFile) throws Exception {

        final InputStream is = getResource(configFile);
        final PatchConfig patchConfig = PatchConfigXml.parse(is);
        assertNotNull(patchConfig.getAppliesToProduct());
        assertNotNull(patchConfig.getAppliesToVersion());
        assertEquals(1, patchConfig.getElements().size());
        assertFalse(patchConfig.getSpecifiedContent().isEmpty());

        final PatchElementConfig elementConfig = patchConfig.getElements().iterator().next();
        assertNotNull(elementConfig.getPatchId());
        assertNotNull(elementConfig.getDescription());
        assertNotNull(elementConfig.getPatchType());
        assertFalse(elementConfig.getSpecifiedContent().isEmpty());

//
//        // Validate the structure
//
//
//        DistributionContentItem modulesBase = DistributionContentItem.createMiscItemForPath("test");
//        modulesBase = new DistributionContentItem("modules", DistributionContentItem.Type.MODULE_PARENT, modulesBase, true);
//        DistributionContentItem modulesTest = new DistributionContentItem("dir", DistributionContentItem.Type.MODULE_PARENT, modulesBase, true);
//        modulesTest = new DistributionContentItem("main", DistributionContentItem.Type.MODULE_ROOT, modulesTest, true);
//        DistributionStructure.SlottedContentSearchPath searchPath = originalDS.getModuleSearchPath(modulesTest);
//        assertEquals("test", searchPath.getName());
//        assertEquals(modulesBase, searchPath.getPath());
//        searchPath = updatedDS.getModuleSearchPath(modulesTest);
//        assertEquals("test", searchPath.getName());
//        assertEquals(modulesBase, searchPath.getPath());
//
//        modulesBase = new DistributionContentItem("modules", DistributionContentItem.Type.MODULE_PARENT, DistributionContentItem.createDistributionRoot(), true);
//        modulesTest = new DistributionContentItem("dir", DistributionContentItem.Type.MODULE_PARENT, modulesBase, true);
//        modulesTest = new DistributionContentItem("main", DistributionContentItem.Type.MODULE_ROOT, modulesTest, true);
//
//        searchPath = originalDS.getModuleSearchPath(modulesTest);
//        assertEquals("", searchPath.getName());
//        assertEquals(modulesBase, searchPath.getPath());
//
//        try {
//            updatedDS.getModuleSearchPath(modulesTest);
//            fail("defaul module path not removed");
//        } catch (IllegalArgumentException e) {
//            // good
//        }
//
//        DistributionContentItem bundlesBase = DistributionContentItem.createMiscItemForPath("bundles");
//        bundlesBase = new DistributionContentItem("test", DistributionContentItem.Type.BUNDLE_PARENT, bundlesBase, true);
//        DistributionContentItem bundlesTest = new DistributionContentItem("dir", DistributionContentItem.Type.BUNDLE_PARENT, bundlesBase, true);
//        bundlesTest = new DistributionContentItem("main", DistributionContentItem.Type.BUNDLE_ROOT, bundlesTest, true);
//        searchPath = originalDS.getBundleSearchPath(bundlesTest);
//        assertEquals("test", searchPath.getName());
//        assertEquals(bundlesBase, searchPath.getPath());
//        searchPath = updatedDS.getBundleSearchPath(bundlesTest);
//        assertEquals("test", searchPath.getName());
//        assertEquals(bundlesBase, searchPath.getPath());
//
//        bundlesBase = new DistributionContentItem("bundles", DistributionContentItem.Type.BUNDLE_PARENT, DistributionContentItem.createDistributionRoot(), true);
//        bundlesTest = new DistributionContentItem("dir", DistributionContentItem.Type.BUNDLE_PARENT, bundlesBase, true);
//        bundlesTest = new DistributionContentItem("main", DistributionContentItem.Type.BUNDLE_ROOT, bundlesTest, true);
//        searchPath = originalDS.getBundleSearchPath(bundlesTest);
//        assertEquals("", searchPath.getName());
//        assertEquals(bundlesBase, searchPath.getPath());
//
//        try {
//            updatedDS.getBundleSearchPath(bundlesTest);
//            fail("defaul bundle path not removed");
//        } catch (IllegalArgumentException e) {
//            // good
//        }
//
//        DistributionContentItem misc = DistributionContentItem.createMiscItemForPath("misc");
//        File testFile = new File("test");
//        DistributionContentItem testee = originalDS.getContentItem(testFile, misc);
//        assertEquals(DistributionContentItem.Type.IGNORED, testee.getType());
//        testee = updatedDS.getContentItem(testFile, misc);
//        assertEquals(DistributionContentItem.Type.IGNORED, testee.getType());
//
//        DistributionContentItem test = DistributionContentItem.createMiscItemForPath("test");
//        File miscFile = new File("misc");
//        testee = originalDS.getContentItem(miscFile, test);
//        assertEquals(DistributionContentItem.Type.IGNORED, testee.getType());
//        testee = updatedDS.getContentItem(miscFile, test);
//        assertEquals(DistributionContentItem.Type.MISC, testee.getType());
//
//        Map<ModificationType, Set<String>> content = new HashMap<ModificationType, Set<String>>();
//        Set<String> adds = new HashSet<String>(Arrays.asList("modules/org/jboss/as/test/main", "test/modules/org/jboss/as/test/prod",
//                "bundles/test/org/jboss/as/test/main", "bundles/org/jboss/as/test/prod", "test/file", "test/file2"));
//        content.put(ModificationType.ADD, adds);
//        Set<String> mods = new HashSet<String>(Arrays.asList("test/modules/org/jboss/as/test2/main", "modules/org/jboss/as/test2/prod",
//                "bundles/org/jboss/as/test2/main", "bundles/test/org/jboss/as/test2/prod", "test/file3", "test/file4"));
//        content.put(ModificationType.MODIFY, mods);
//        Set<String> rems = new HashSet<String>(Arrays.asList("modules/org/jboss/as/test3/main", "test/modules/org/jboss/as/test3/prod",
//                "bundles/org/jboss/as/test3/main", "bundles/test/org/jboss/as/test3/prod", "test/file5", "test/file6"));
//        content.put(ModificationType.REMOVE, rems);
//
//        for (Map<ModificationType, SortedSet<DistributionContentItem>> map : patchConfig.getSpecifiedContent().values()) {
//            for (Map.Entry<ModificationType, SortedSet<DistributionContentItem>> entry : map.entrySet()) {
//                for (DistributionContentItem item : entry.getValue()) {
//                    String path = item.getPath();
//                    assertTrue(path, content.get(entry.getKey()).remove(path));
//                }
//            }
//        }
//
//        for (Set<String> set : content.values()) {
//            assertTrue(set.toString(), set.isEmpty());
//        }
//
//        Set<String> validInUse = new HashSet<String>(Arrays.asList("test/file3", "test/file5"));
//        validateInRuntimeUse(patchConfig, validInUse);
    }

    private static InputStream getResource(String name) throws IOException {
        final URL resource = PatchConfigXmlUnitTestCase.class.getClassLoader().getResource(name);
        assertNotNull(name, resource);
        return resource.openStream();
    }

    private static void validateAppliesTo(final PatchConfig patchConfig, String... expected) {
        Set<String> expectedAppliesTo = new HashSet<String>(Arrays.asList(expected));
        Set<String> actualAppliesTo = new HashSet<String>(Arrays.asList(patchConfig.getAppliesToVersion()));

        assertEquals(expectedAppliesTo, actualAppliesTo);
    }

    private static void validateInRuntimeUse(final PatchConfig patchConfig) {
        Set<String> validInUse = new HashSet<String>(Arrays.asList("test", "test/file", "test/file/file1"));
        validateInRuntimeUse(patchConfig, validInUse);
    }

    private static void validateInRuntimeUse(final PatchConfig patchConfig, Set<String> validInUse) {
        for (String path : patchConfig.getInRuntimeUseItems()) {
            assertTrue(path + " is valid", validInUse.remove(path));
        }
        assertEquals("Expected in-runtime-use item not found", Collections.emptySet(), validInUse);
    }

}