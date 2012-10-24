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

package org.jboss.as.patching.runner;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static org.jboss.as.patching.metadata.Patch.PatchType.CUMULATIVE;
import static org.jboss.as.patching.runner.PatchUtils.bytesToHexString;
import static org.jboss.as.patching.runner.PatchUtils.calculateHash;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.Patch;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public class PatchingAssert {

    static void assertContains(File f, File... files) {
        List<File> set = Arrays.asList(files);
        assertTrue(f + " not found in " + set, set.contains(f));
    }

    static File assertDirExists(File rootDir, String... segments) {
        return assertFileExists(true, rootDir, segments);
    }

    static void assertDirDoesNotExist(File rootDir, String... segments) {
        assertFileDoesNotExist(rootDir, segments);
    }

    static File assertFileExists(File rootDir, String... segments) {
        return assertFileExists(false, rootDir, segments);
    }

    private static File assertFileExists(boolean isDir, File rootFile, String... segments) {
        assertTrue(rootFile + " does not exist", rootFile.exists());
        File f = rootFile;
        for (String segment : segments) {
            f = new File(f, segment);
            assertTrue(f + " does not exist", f.exists());
        }
        assertEquals(f + " is " + (isDir? "not":"") + " a directory", isDir, f.isDirectory());
        return f;
    }

    static void assertFileDoesNotExist(File rootFile, String... segments) {
        if (segments.length == 0) {
            assertFalse(rootFile + " exists", rootFile.exists());
            return;
        }

        File f = rootFile;
        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i];
            f = new File(f, segment);
            assertTrue(f + " does not exist", f.exists());
        }
        f = new File(f, segments[segments.length -1]);
    }

    static void assertFileContent(byte[] expected, File f) throws Exception {
        assertFileContent(null, expected, f);
    }

    static void assertFileContent(String message, byte[] expected, File f) throws Exception {
        assertEquals(message, bytesToHexString(expected), bytesToHexString(calculateHash(f)));
    }

    static void assertDefinedModule(File[] modulesPath, String moduleName, byte[] expectedHash) throws Exception {
        for (File path : modulesPath) {
            final File modulePath = PatchContentLoader.getModulePath(path, moduleName, "main");
            final File moduleXml = new File(modulePath, "module.xml");
            if (moduleXml.exists()) {
                assertDefinedModuleWithRootElement(moduleXml, moduleName, "<module");
                if (expectedHash != null) {
                    byte[] actualHash = PatchUtils.calculateHash(modulePath);
                    assertTrue("content of module differs", Arrays.equals(expectedHash, actualHash));
                }
                return;
            }
        }
        fail("count not find module for " + moduleName + " in " + asList(modulesPath));
    }

    static void assertDefinedBundle(File[] bundlesPath, String moduleName, byte[] expectedHash) throws Exception {
        for (File path : bundlesPath) {
            final File bundlePath = PatchContentLoader.getModulePath(path, moduleName, "main");
            if(bundlePath.exists()) {
                if(expectedHash != null) {
                    byte[] actualHash = PatchUtils.calculateHash(bundlePath);
                    assertTrue("content of bundle differs", Arrays.equals(expectedHash, actualHash));
                }
                return;
            }
        }
        fail("content not found bundle for " + moduleName + " in " + asList(bundlesPath));
    }

    static void assertDefinedAbsentBundle(File[] bundlesPath, String moduleName) throws Exception {
        for (File path : bundlesPath) {
            final File bundlePath = PatchContentLoader.getModulePath(path, moduleName, "main");
            if(bundlePath.exists()) {
                final File[] children = bundlePath.listFiles();
                if(children.length == 0) {
                    return;
                }
            }
        }
        fail("content not found bundle for " + moduleName + " in " + asList(bundlesPath));
    }

    static void assertDefinedAbsentModule(File[] modulesPath, String moduleName) throws Exception {
        for (File path : modulesPath) {
            final File modulePath = PatchContentLoader.getModulePath(path, moduleName, "main");
            final File moduleXml = new File(modulePath, "module.xml");
            if (moduleXml.exists()) {
                assertDefinedModuleWithRootElement(moduleXml, moduleName, "<module-absent ");
                return;
            }
        }
        fail("count not found module for " + moduleName + " in " + asList(modulesPath));
    }

    private static void assertDefinedModuleWithRootElement(File moduleXMLFile, String moduleName, String rootElement) throws Exception {
        assertFileExists(moduleXMLFile);
        assertFileContains(moduleXMLFile,rootElement);
        assertFileContains(moduleXMLFile, format("name=\"%s\"", moduleName));
    }

    private static void assertFileContains(File f, String string) throws Exception {
        String content = new Scanner(f, "UTF-8").useDelimiter("\\Z").next();
        assertTrue(string + " not found in " + f + " with content=" + content, content.contains(string));
    }

    static void assertPatchHasBeenApplied(PatchingResult result, Patch patch) {
        assertFalse("encountered problems: " + result.getProblems(), result.hasFailures());
        if (CUMULATIVE == patch.getPatchType()) {
            assertEquals(patch.getPatchId(), result.getPatchInfo().getCumulativeID());
            assertEquals(patch.getResultingVersion(), result.getPatchInfo().getVersion());
        } else {
            assertTrue(result.getPatchInfo().getPatchIDs().contains(patch.getPatchId()));
            // applied one-off patch is at the top of the patchIDs
            assertEquals(patch.getPatchId(), result.getPatchInfo().getPatchIDs().get(0));
        }
    }

    static void assertPatchHasNotBeenApplied(PatchingResult result, Patch patch, ContentItem problematicItem) {
        assertTrue("patch should have failed", result.hasFailures());
        assertTrue(problematicItem + " is not reported in the problemes " + result.getProblems(), result.getProblems().contains(problematicItem));

        assertDirDoesNotExist(result.getPatchInfo().getEnvironment().getPatchDirectory(patch.getPatchId()));
        assertDirDoesNotExist(result.getPatchInfo().getEnvironment().getHistoryDir(patch.getPatchId()));
    }

    static void assertPatchHasBeenRolledBack(PatchingResult result, Patch patch, PatchInfo expectedPatchInfo) {
        assertFalse("encountered problems: " + result.getProblems(), result.hasFailures());
        assertEquals(expectedPatchInfo.getVersion(), result.getPatchInfo().getVersion());
        assertEquals(expectedPatchInfo.getCumulativeID(), result.getPatchInfo().getCumulativeID());
        assertEquals(expectedPatchInfo.getPatchIDs(), result.getPatchInfo().getPatchIDs());

        assertNoResourcesForPatch(result.getPatchInfo(), patch);
    }

    static void assertNoResourcesForPatch(PatchInfo patchInfo, Patch patch) {
        assertDirDoesNotExist(patchInfo.getEnvironment().getModulePatchDirectory(patch.getPatchId()));
        assertDirDoesNotExist(patchInfo.getEnvironment().getBundlesPatchDirectory(patch.getPatchId()));
        assertDirDoesNotExist(patchInfo.getEnvironment().getPatchDirectory(patch.getPatchId()));
    }
}
