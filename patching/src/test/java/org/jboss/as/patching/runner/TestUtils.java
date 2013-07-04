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
import static java.util.UUID.randomUUID;
import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.Constants.LAYERS;
import static org.jboss.as.patching.Constants.SYSTEM;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.patching.IoUtils.safeClose;
import static org.jboss.as.patching.PatchLogger.ROOT_LOGGER;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.ZipUtils;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchXml;
import org.jboss.as.protocol.StreamUtils;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public class TestUtils {

    public static String randomString() {
        return randomUUID().toString();
    }

    public static void tree(File dir) {
        StringBuilder out = new StringBuilder();
        out.append(dir.getParentFile().getAbsolutePath() + "\n");
        tree0(out, dir, 1, "  ");
        System.out.println(out);
        ROOT_LOGGER.trace(out.toString());
    }

    private static void tree0(StringBuilder out, File dir, int indent, String tab) {
        StringBuilder shift = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            shift.append(tab);
        }
        out.append(shift + dir.getName() + "\n");
        for (File child : dir.listFiles()) {
            if (child.isDirectory()) {
                tree0(out, child, indent + 1, tab);
            } else {
                out.append(shift + tab + child.getName() + "\n");
            }
        }
    }

    public static File touch(File baseDir, String... segments) throws IOException {
        File f = baseDir;
        for (String segment : segments) {
            f = new File(f, segment);
        }
        f.getParentFile().mkdirs();
        f.createNewFile();
        return f;
    }

    public static void dump(File f, String content) throws IOException {
        final OutputStream os = new FileOutputStream(f);
        try {
            os.write(content.getBytes(Charset.forName("UTF-8")));
            os.close();
        } finally {
            IoUtils.safeClose(os);
        }
    }

    public static File createModuleXmlFile(File mainDir, String moduleSpec, String... resources) throws IOException {
        final int c1 = moduleSpec.lastIndexOf(':');
        final String name;
        final String slot;
        if (c1 != -1) {
            name = moduleSpec.substring(0, c1);
            slot = moduleSpec.substring(c1 + 1);
        } else {
            name = moduleSpec;
            slot = "main";
        }

        StringBuilder content = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        content.append(format("<module xmlns=\"urn:jboss:module:1.2\" name=\"%s\" slot=\"%s\">\n", name, slot));
        content.append("  <resources>\n");
        content.append("    <resource-root path=\".\"/>\n");
        for (String resource : resources) {
            content.append(format("    <resource-root path=\"%s\"/>\n", resource));
        }
        content.append("  </resources>\n");
        content.append("</module>\n");
        ROOT_LOGGER.trace(content);
        File moduleXMLFile = touch(mainDir, "module.xml");
        dump(moduleXMLFile, content.toString());
        return moduleXMLFile;
    }

    public static File createModule(File baseDir, String moduleName, String... resourcesContents) throws IOException {
        File moduleDir = IoUtils.mkdir(baseDir, "modules", moduleName);
        File mainDir = IoUtils.mkdir(moduleDir, "main");
        String resourceFilePrefix = randomString();
        String[] resourceFileNames = new String[resourcesContents.length];
        for (int i = 0; i < resourcesContents.length; i++) {
            String content = resourcesContents[i];
            String fileName = resourceFilePrefix + "-" + i;
            resourceFileNames[i] = fileName;
            File f = touch(mainDir, fileName);
            dump(f, content);
        }
        createModuleXmlFile(mainDir, moduleName, resourceFileNames);
        return moduleDir;
    }

    public static File createModule0(File baseDir, String moduleName, String... resourcesContents) throws IOException {
        File mainDir = createModuleRoot(baseDir, moduleName);
        String resourceFilePrefix = randomString();
        String[] resourceFileNames = new String[resourcesContents.length];
        for (int i = 0; i < resourcesContents.length; i++) {
            String content = resourcesContents[i];
            String fileName = resourceFilePrefix + "-" + i;
            resourceFileNames[i] = fileName;
            File f = touch(mainDir, fileName);
            dump(f, content);
        }
        createModuleXmlFile(mainDir, moduleName, resourceFileNames);
        return mainDir.getParentFile();
    }

    public static File createModule1(File baseDir, String moduleName, String... resourceFileNames) throws IOException {
        File mainDir = createModuleRoot(baseDir, moduleName);
        createModuleXmlFile(mainDir, moduleName, resourceFileNames);
        return mainDir.getParentFile();
    }

    public static File createModuleRoot(File baseDir, String moduleSpec) throws IOException {
        final int c1 = moduleSpec.lastIndexOf(':');
        final String name;
        final String slot;
        if (c1 != -1) {
            name = moduleSpec.substring(0, c1);
            slot = moduleSpec.substring(c1 + 1);
        } else {
            name = moduleSpec;
            slot = "main";
        }
        assert slot.equals(ModuleItem.MAIN_SLOT); // update to support other slots too
        final String[] segments = name.split("\\.");
        assert segments.length > 0;
        File dir = baseDir;
        for (String segment : segments) {
            dir = new File(dir, segment);
        }
        dir = new File(dir, slot);
        dir.mkdirs();
        return dir;
    }

    public static File createBundle0(File baseDir, String bundleName, String content) throws IOException {
        File mainDir = createModuleRoot(baseDir, bundleName);
        if(content != null) {
            File f = touch(mainDir, "content");
            dump(f, content);
        }
        return mainDir.getParentFile();
    }

    public static void createPatchXMLFile(File dir, Patch patch) throws Exception {
        createPatchXMLFile(dir, patch, false);
    }

    public static void createPatchXMLFile(File dir, Patch patch, boolean logContent) throws Exception {
        File patchXMLfile = new File(dir, "patch.xml");
        patchXMLfile.createNewFile();
        FileOutputStream fos = new FileOutputStream(patchXMLfile);
        try {
            PatchXml.marshal(fos, patch);
        } finally {
            safeClose(fos);
        }

        if(logContent) {
            java.io.FileInputStream fis = null;
            try {
                fis = new java.io.FileInputStream(new java.io.File(dir, "patch.xml"));
                final byte[] bytes = new byte[fis.available()];
                fis.read(bytes);
                System.out.println(new String(bytes));
            } finally {
                StreamUtils.safeClose(fis);
            }
        }
    }

    public static File createZippedPatchFile(File sourceDir, String zipFileName) {
        tree(sourceDir);
        File zipFile = new File(sourceDir.getParent(), zipFileName + ".zip");
        ZipUtils.zip(sourceDir, zipFile);
        return zipFile;
    }

    /**
     * Creates (a part of) the distribution on the filesystem necessary to the run the tests.
     *
     * @param env  the directory structure to be created
     * @param identity  the identity name
     * @param productName  release name
     * @param productVersion  release version
     * @return  the bin directory
     * @throws Exception  if anything goes wrong
     */
    public static File createInstalledImage(DirectoryStructure env, String identity, String productName, String productVersion) throws Exception {
        // start from a base installation
        // with a file in it
        File binDir = mkdir(env.getInstalledImage().getJbossHome(), "bin");

        // create product.conf
        File productConf = new File(binDir, "product.conf");
        assertTrue("Failed to create product.conf", productConf.createNewFile());
        Properties props = new Properties();
        props.setProperty("slot", identity);
        FileWriter writer = null;
        try {
            writer = new FileWriter(productConf);
            props.store(writer, null);
        } finally {
            StreamUtils.safeClose(writer);
        }

        // create the product module
        final File modulesDir = newFile(env.getInstalledImage().getModulesDir(), SYSTEM, LAYERS, BASE);
        if(!modulesDir.exists()) {
            modulesDir.mkdirs();
        }
        final File moduleDir = TestUtils.createModule1(modulesDir, "org.jboss.as.product:" + identity, "product.jar");

        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue(Name.MANIFEST_VERSION.toString(), "xxx");
        manifest.getMainAttributes().putValue("JBoss-Product-Release-Name", productName);
        manifest.getMainAttributes().putValue("JBoss-Product-Release-Version", productVersion);

        final File moduleJar = new File(new File(moduleDir, identity), "product.jar");
        JarOutputStream jar = null;
        try {
            jar = new JarOutputStream(new FileOutputStream(moduleJar), manifest);
            jar.flush();
        } finally {
            StreamUtils.safeClose(jar);
        }
        return binDir;
    }
}
