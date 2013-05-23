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
import static org.jboss.as.patching.IoUtils.safeClose;
import static org.jboss.as.patching.PatchLogger.ROOT_LOGGER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.ZipUtils;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchXml;

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

    public static File mkdir(File parent, String... segments) throws Exception {
        File dir = parent;
        for (String segment : segments) {
            dir = new File(dir, segment);
        }
        dir.mkdirs();
        return dir;
    }

    public static File touch(File baseDir, String... segments) throws Exception {
        File f = baseDir;
        for (String segment : segments) {
            f = new File(f, segment);
        }
        f.getParentFile().mkdirs();
        f.createNewFile();
        return f;
    }

    public static void dump(File f, String content) throws Exception {
        final OutputStream os = new FileOutputStream(f);
        try {
            os.write(content.getBytes(Charset.forName("UTF-8")));
            os.close();
        } finally {
            IoUtils.safeClose(os);
        }
    }

    public static File createModuleXmlFile(File mainDir, String moduleName, String... resources) throws Exception {
        StringBuilder content = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        content.append(format("<module xmlns=\"urn:jboss:module:1.2\" name=\"%s\" slot=\"main\" />\n", moduleName));
        content.append("  <resources>\n");
        content.append("    resource-root path=\".\"/>\n");
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

    public static File createModule(File baseDir, String moduleName, String... resourcesContents) throws Exception {
        File moduleDir = mkdir(baseDir, "modules", moduleName);
        File mainDir = mkdir(moduleDir, "main");
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

    public static File createBundle(File baseDir, String moduleName, boolean content) throws Exception {
        File bundles = mkdir(baseDir, "bundles", moduleName);
        File mainDir = mkdir(bundles, "main");
        if(content) {
            File f = touch(mainDir, "content");
            dump(f, randomString());
        }
        return bundles;
    }

    static void createPatchXMLFile(File dir, Patch patch) throws Exception {
        File patchXMLfile = new File(dir, "patch.xml");
        patchXMLfile.createNewFile();
        FileOutputStream fos = new FileOutputStream(patchXMLfile);
        try {
            PatchXml.marshal(fos, patch);
        } finally {
            safeClose(fos);
        }
    }

    static File createZippedPatchFile(File sourceDir, String zipFileName) {
        tree(sourceDir);
        File zipFile = new File(sourceDir.getParent(), zipFileName + ".zip");
        ZipUtils.zip(sourceDir, zipFile);
        return zipFile;
    }

    static File[] getModulePath(final DirectoryStructure structure, final PatchInfo info) {
        final List<File> path = new ArrayList<File>();
        final List<String> patches = info.getPatchIDs();
        for (final String patch : patches) {
            path.add(structure.getModulePatchDirectory(patch));
        }
        final String ref = info.getCumulativeID();
        if (!BASE.equals(ref)) {
            path.add(structure.getModulePatchDirectory(ref));
        }
        path.add(structure.getModuleRoot());
        return path.toArray(new File[path.size()]);
    }

    static File[] getBundlePath(final DirectoryStructure structure, final PatchInfo info) {
        final List<String> patches = info.getPatchIDs();
        final List<File> path = new ArrayList<File>();
        for (final String patch : patches) {
            path.add(structure.getBundlesPatchDirectory(patch));
        }
        final String ref = info.getCumulativeID();
        if (!BASE.equals(ref)) {
            path.add(structure.getBundlesPatchDirectory(ref));
        }
        path.add(structure.getBundleRepositoryRoot());
        return path.toArray(new File[path.size()]);
    }


}
