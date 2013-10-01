/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.patching.generator.PatchGenerator.processingError;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.ZipUtils;
import org.jboss.as.patching.metadata.MultiPatch;
import org.jboss.as.patching.metadata.MultiPatchXml;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchXml;

/**
 * @author Emanuel Muckenhuber
 */
class PatchBundleGenerator {

    private static final String LF = "\r\n";
    private File tmp;

    public static void assemble(final String... args) throws Exception {

        String patch = null;
        String existing = null;
        String output = null;

        final int argsLength = args.length;
        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];
            try {
                if ("--help".equals(arg) || "-h".equals(arg) || "-H".equals(arg)) {
                    usage();
                    return;
                } else if (arg.startsWith("--patch=")) {
                    patch = arg.substring("--patch=".length());
                } else if (arg.startsWith("--existing=")) {
                    existing = arg.substring("--existing=".length());
                } else if (arg.startsWith("--output=")) {
                    output = arg.substring("--output=".length());
                } else if (arg.equals("--assemble-patch-bundle")) {
                    continue;
                } else {
                    System.err.println(PatchMessages.MESSAGES.argumentExpected(arg));
                    usage();
                    return;
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.println(PatchMessages.MESSAGES.argumentExpected(arg));
                usage();
                return;
            }
        }

        final PatchBundleGenerator gen = new PatchBundleGenerator();
        gen.createTempStructure(UUID.randomUUID().toString());

        final File p = new File(patch);
        final File e = existing == null ? null : new File(existing);
        final File t = new File(output);

        try {
            gen.assemble(p, e, t);
        } finally {
            IoUtils.recursiveDelete(gen.tmp);
        }
    }

    public void assemble(final File patch, final File existing, final File target) throws IOException, XMLStreamException, PatchingException {

        final File multiPatchContent = new File(tmp, "patch-bundle-content");
        multiPatchContent.mkdir();
        final File multiPatchXml = new File(multiPatchContent, MultiPatchXml.MULTI_PATCH_XML);
        final MultiPatch metadata;
        if (existing != null && existing.exists()) {

            ZipUtils.unzip(existing, multiPatchContent);

            final InputStream is = new FileInputStream(multiPatchXml);
            try {
                metadata = MultiPatchXml.parse(is);
            } finally {
                IoUtils.safeClose(is);
            }
        } else {
            metadata = new MultiPatch() {
                @Override
                public List<MultiPatchEntry> getPatches() {
                    return Collections.emptyList();
                }
            };
        }

        final File patchContent = new File(tmp, "patch-content");
        ZipUtils.unzip(patch, patchContent);

        final File patchXml = new File(patchContent, PatchXml.PATCH_XML);
        final Patch patchMetadata = PatchXml.parse(patchXml).resolvePatch(null, null);
        final String patchID = patchMetadata.getPatchId();
        final String patchPath = patchID + ".zip";

        final List<MultiPatch.MultiPatchEntry> entries = new ArrayList<MultiPatch.MultiPatchEntry>(metadata.getPatches());
        entries.add(new MultiPatch.MultiPatchEntry(patchID, patchPath));

        final File patchTarget = new File(multiPatchContent, patchPath);
        IoUtils.copyFile(patch, patchTarget);

        final OutputStream os = new FileOutputStream(multiPatchXml);
        try {
            MultiPatchXml.marshal(os, new MultiPatch() {
                @Override
                public List<MultiPatchEntry> getPatches() {
                    return entries;
                }
            });
        } finally {
            IoUtils.safeClose(os);
        }

        ZipUtils.zip(multiPatchContent, target);
    }

    private void createTempStructure(String patchId) {

        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        int count = 0;
        while (tmp == null || tmp.exists()) {
            count++;
            tmp = new File(tmpDir, "jboss-as-patch-" + patchId + "-" + count);
        }
        if (!tmp.mkdirs()) {
            throw processingError("Cannot create tmp dir for patch create at %s", tmp.getAbsolutePath());
        }
        tmp.deleteOnExit();
    }

    static void usage() {
        final StringBuilder builder = new StringBuilder();
        builder.append("USAGE:").append(LF);
        builder.append("patch-gen.sh --assemble-patch-bundle --patch=/path/to/the/patch --existing=/path/to/existing/patch/bundle --output=/path/to/the/output").append(LF);
        System.err.println(builder.toString());
    }

}
