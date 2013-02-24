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

package org.jboss.as.osgi.service;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests of {@link LayeredBundlePathFactory} when "layers" and "add-ons" are configured.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class LayeredBundlePathTest {

    private static final String PATH = "layeredbundlepath/";

    private File reposRoot;
    private File repoA;
    private File repoB;

    @Before
    public void setUp() throws Exception {

        reposRoot = new File(getResource(PATH), "repos");
        if (!reposRoot.mkdirs() && !reposRoot.isDirectory()) {
            throw new IllegalStateException("Cannot create reposRoot");
        }
        repoA = new File(reposRoot, "root-a");
        if (!repoA.mkdirs() && !repoA.isDirectory()) {
            throw new IllegalStateException("Cannot create reposA");
        }
        repoB = new File(reposRoot, "root-b");
        if (!repoB.mkdirs() && !repoB.isDirectory()) {
            throw new IllegalStateException("Cannot create reposB");
        }
    }

    @After
    public void tearDown() throws Exception {
        if (reposRoot != null) {
            cleanFile(reposRoot);
        }
    }

    private void cleanFile(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                cleanFile(child);
            }
        }
        if (!file.delete() && file.exists()) {
            file.deleteOnExit();
        }
    }

    @Test
    public void testBaseLayer() throws Exception {
        createRepo("root-a", false, false, Collections.singletonList("base"));

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, false, "base");
    }

    @Test
    public void testSpecifiedBaseLayer() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", false, false, "base");

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, false, "base");
    }

    @Test
    public void testSimpleOverlay() throws Exception {
        createRepo("root-a", false, false, Collections.singletonList("base"), "top");

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, false, "top", "base");
    }

    @Test
    public void testSpecifiedBaseLayerWithSimpleOverlay() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", false, false, "top", "base");

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, false, "top", "base");
    }

    @Test
    public void testMultipleOverlays() throws Exception {
        createRepo("root-a", false, false, Collections.singletonList("base"), "top", "mid");

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, false, "top", "mid", "base");
    }

    @Test
    public void testSpecifiedBaseLayerWithMultipleOverlays() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", false, false, "top", "mid", "base");

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, false, "top", "mid", "base");
    }

    @Test
    public void testBasePlusAddOns() throws Exception {
        createRepo("root-a", true, false, Collections.singletonList("base"));

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, true, "base");
    }

    @Test
    public void testSpecifiedBasePlusAddOns() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", true, false, "base");

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, true, "base");
    }

    @Test
    public void testLayersAndAddOns() throws Exception {
        createRepo("root-a", true, false, Collections.singletonList("base"), "top", "mid");

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, true, "top", "mid", "base");
    }

    @Test
    public void testSpecifiedBaseLayersAndAddOns() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", true, false, "top", "mid", "base");

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, true, "top", "mid", "base");
    }

    @Test
    public void testBaseLayerAndUser() throws Exception {
        createRepo("root-a", false, false, Collections.singletonList("base"));

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, false, "base");
    }

    @Test
    public void testSpecifiedBaseLayerAndUser() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", false, true, "base");

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, false, "base");
    }

    @Test
    public void testSingleRootComplete() throws Exception {
        createRepo("root-a", true, false, Collections.singletonList("base"), "top", "mid");

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, true, "top", "mid", "base");
    }

    @Test
    public void testSpecifiedBaseSingleRootComplete() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", true, true, "top", "mid", "base");

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, true, "top", "mid", "base");
    }

    @Test
    public void testSecondRepoHigherPrecedence() throws Exception {
        createRepo("root-a", false, false, Collections.singletonList("base"));
        createRepo("root-b", false, true);

        File[] standardPath = { repoB, repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 1, 0, false, "base");
    }

    @Test
    public void testSecondRepoLowerPrecedence() throws Exception {
        createRepo("root-a", false, false, Collections.singletonList("base"));
        createRepo("root-b", false, true);

        File[] standardPath = { repoA, repoB };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, 2, false, "base");
    }

    @Test
    public void testExtraneousOverlay() throws Exception {
        createRepo("root-a", false, false, Arrays.asList("base", "mid"), "top");

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, false, "top", "base");
    }

    @Test
    public void testSpecifiedBaseLayerWithExtraneousOverlay() throws Exception {
        // This setup puts "base" in layers.conf
        createRepo("root-a", false, false, Arrays.asList("mid"), "top", "base");

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, false, "top", "base");
    }

    /** Tests that setting up add-ons has no effect without the layers structure */
    @Test
    public void testLayersRequiredForAddOns() throws Exception {
        createRepo("root-a", true, false);

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, false);

        // Now add the layers/base dir
        new File(repoA, "system/layers/base").mkdirs();

        bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, true, "base");
    }

    @Test
    public void testRejectConfWithNoStructure() throws Exception {
        createRepo("root-a", false, false);
        writeLayersConf("root-a", "top");

        File[] standardPath = { repoA };
        try {
            LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
            Assert.fail("layers.conf with no layers should fail");
        } catch (Exception good) {
            // good
        }
    }

    @Test
    public void testRejectConfWithMissingLayer() throws Exception {
        createRepo("root-a", false, false, Arrays.asList("top", "base"));
        writeLayersConf("root-a", "top", "mid");

        File[] standardPath = { repoA };
        try {
            LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
            Assert.fail("layers.conf with no layers should fail");
        } catch (Exception good) {
            // good
        }
    }

    @Test
    public void testEmptyLayersConf() throws Exception {
        createRepo("root-a", false, false, Collections.singletonList("base"));
        writeLayersConf("root-a");

        File[] standardPath = { repoA };
        List<File> bundlePath = LayeredBundlePathFactory.resolveLayeredBundlePath(standardPath);
        validateBundlePath(bundlePath, repoA, 0, -1, false, "base");
    }

    private void writeLayersConf(String rootName, String... layers) throws IOException {
        if (layers != null && layers.length > 0) {

            StringBuilder sb = new StringBuilder("layers=");
            for (int i = 0; i < layers.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(layers[i]);
            }

            File repo = "root-a".equals(rootName) ? repoA : repoB;
            File layersConf = new File(repo, "layers.conf");
            layersConf.createNewFile();
            FileWriter fw = new FileWriter(layersConf);
            try {
                PrintWriter pw = new PrintWriter(fw);
                pw.println(sb.toString());
                pw.close();
            } finally {
                try {
                    fw.close();
                } catch (Exception e) {
                    // meh
                }
            }
        }
    }

    private void createRepo(String rootName, boolean includeAddons, boolean includeUser, String... layers) throws Exception {
        List<String> empty = Collections.emptyList();
        createRepo(rootName, includeAddons, includeUser, empty, layers);
    }

    private void createRepo(String rootName, boolean includeAddons, boolean includeUser, List<String> extraLayers, String... layers) throws Exception {
        if (layers != null && layers.length > 0) {
            writeLayersConf(rootName, layers);
            for (String layer : layers) {
                createLayer(rootName, layer);
            }
        }
        if (extraLayers != null) {
            for (String extraLayer : extraLayers) {
                createLayer(rootName, extraLayer);
            }
        }

        if (includeAddons) {
            createAddOn(rootName, "a");
            createAddOn(rootName, "b");
        }

        if (includeUser) {
            createUserBundles(rootName);
        }
    }

    private void createLayer(String rootName, String layerName) throws Exception {
        createBundles("layers/" + layerName, rootName + "/system/layers/" + layerName, layerName);
    }

    private void createAddOn(String rootName, String addOnName) throws Exception {
        createBundles("add-ons/" + addOnName, rootName + "/system/add-ons/" + addOnName, addOnName);
    }

    private void createUserBundles(String rootName) throws Exception {
        createBundles("user", rootName, "user");
    }

    private void createBundles(String sourcePath, String relativeRepoPath, String uniqueName) throws Exception {
        copyResource(PATH + sourcePath + "/shared/textnota.jar", PATH, "repos/" + relativeRepoPath + "/test/shared/main");
        copyResource(PATH + sourcePath + "/unique/textnota.jar", PATH, "repos/" + relativeRepoPath + "/test/" + uniqueName + "/main");
    }

    private void validateBundlePath(List<File> bundlePathList, File repoRoot, int expectedStartPos,
                                    int expectedOtherRootPos, boolean expectAddons, String... layers) {

        final File[] bundlePath = bundlePathList.toArray(new File[bundlePathList.size()]);
        int expectedLength = 1 + layers.length + (expectAddons ? 2 : 0);

        // Validate positional parameters -- check for bad test writers ;)
        if (expectedOtherRootPos < 0) {
            Assert.assertEquals(0, expectedStartPos); //
        } else if (expectedStartPos == 0) {
            Assert.assertEquals(expectedLength, expectedOtherRootPos);
        }

        if (expectedOtherRootPos < 1) {
            Assert.assertEquals("Correct bundle path length", expectedStartPos + expectedLength, bundlePath.length);
        } else {
            Assert.assertTrue("Correct bundle path length", bundlePath.length > expectedStartPos + expectedLength);
        }

        Assert.assertEquals(repoRoot, bundlePath[expectedStartPos]);
        for (int i = 0; i < layers.length; i++) {
            File layer = new File(repoRoot, "system/layers/" + layers[i]);
            Assert.assertEquals(layer, bundlePath[expectedStartPos + i + 1]);
        }
        if (expectAddons) {
            File addOnBase = new File(repoRoot, "system/add-ons");
            Set<String> valid = new HashSet<String>(Arrays.asList("a", "b"));
            for (int i = 0; i < 2; i++) {
                File addOn = bundlePath[expectedStartPos + layers.length + i + 1];
                Assert.assertEquals(addOnBase, addOn.getParentFile());
                String addOnName = addOn.getName();
                Assert.assertTrue(addOnName, valid.remove(addOnName));
            }

        }

        if (expectedOtherRootPos == 0) {
            for (int i = 0; i < expectedStartPos; i++) {
                validateNotChild(bundlePath[i], repoRoot);
            }
        } else if (expectedOtherRootPos > 0) {
            for (int i = expectedOtherRootPos; i < bundlePath.length; i++) {
                validateNotChild(bundlePath[i], repoRoot);
            }
        }

    }

    private void validateNotChild(File file, File repoRoot) {
        File stop = repoRoot.getParentFile();
        File testee = file;
        while (testee != null && !testee.equals(stop)) {
            Assert.assertFalse(testee.equals(repoRoot));
            testee = testee.getParentFile();
        }
    }

    private File getResource(final String path) throws Exception {
        return getResourceFile(getClass(), path);
    }

    private void copyResource(final String inputResource, final String outputBase, final String outputPath) throws Exception {
        final File resource = getResource(inputResource);
        final File outputDirectory = new File(getResource(outputBase), outputPath);

        if(!resource.exists())
            throw new IllegalArgumentException("Resource does not exist");
        if (outputDirectory.exists() && outputDirectory.isFile())
            throw new IllegalArgumentException("OutputDirectory must be a directory");
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs())
                throw new RuntimeException("Failed to create output directory");
        }
        final File outputFile = new File(outputDirectory, resource.getName());
        final InputStream in = new FileInputStream(resource);
        try {
            final OutputStream out = new FileOutputStream(outputFile);
            try {
                final byte[] b = new byte[8192];
                int c;
                while ((c = in.read(b)) != -1) {
                    out.write(b, 0, c);
                }
                out.close();
                in.close();
            } finally {
                safeClose(out);
            }
        } finally {
            safeClose(in);
        }
    }

    public static URL getResource(final Class<?> baseClass, final String path) throws Exception {
        return baseClass.getClassLoader().getResource(path);
    }

    public static File getResourceFile(final Class<?> baseClass, final String path) throws Exception {
        URL url = getResource(baseClass, path);
        URI uri = url.toURI();
        return new File(uri);
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (IOException e) {
            // meh
        }
    }
}
