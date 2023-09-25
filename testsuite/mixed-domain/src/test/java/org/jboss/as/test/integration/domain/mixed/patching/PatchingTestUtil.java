/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.patching;

import static org.jboss.as.patching.IoUtils.safeClose;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.ZipUtils;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchXml;

/**
 * @author Jan Martiska, Jeff Mesnil
 */
public class PatchingTestUtil {


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
            os.write(content.getBytes(StandardCharsets.UTF_8));
            os.close();
        } finally {
            IoUtils.safeClose(os);
        }
    }

    public static void dump(File f, byte[] content) throws IOException {
        final OutputStream os = new FileOutputStream(f);
        try {
            os.write(content);
            os.close();
        } finally {
            IoUtils.safeClose(os);
        }
    }

    public static void createPatchXMLFile(File dir, Patch patch) throws Exception {
        File patchXMLfile = new File(dir, "patch.xml");
        FileOutputStream fos = new FileOutputStream(patchXMLfile);
        try {
            PatchXml.marshal(fos, patch);
        } finally {
            safeClose(fos);
        }
    }

    public static File createZippedPatchFile(File sourceDir, String zipFileName) {
        return createZippedPatchFile(sourceDir, zipFileName, null);
    }

    public static File createZippedPatchFile(File sourceDir, String zipFileName, File targetDir) {
        if (targetDir == null) {
            targetDir = sourceDir.getParentFile();
        }
        File zipFile = new File(targetDir, zipFileName + ".zip");
        ZipUtils.zip(sourceDir, zipFile);
        return zipFile;
    }
}
