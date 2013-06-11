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

import static java.util.UUID.randomUUID;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.ZipUtils;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchElementBuilder;
import org.jboss.as.version.ProductConfig;
import org.jboss.as.version.Usage;
import org.jboss.as.version.Version;
import org.jboss.modules.Module;

/**
 * Generates a patch archive.
 * Run it using JBoss modules:
 * <pre><code>
 *   java -jar jboss-modules.jar -mp modules/ org.jboss.as.patching.generator
 * </code></pre>
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class PatchGenerator {

    public static void main(String[] args) {
        try {
            PatchGenerator patchGenerator = parse(args);
            if (patchGenerator != null) {
                patchGenerator.process();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final File patchConfigFile;
    private File oldRoot;
    private File newRoot;
    private DistributionStructure oldStructure;
    private DistributionStructure newStructure;
    private File patchFile;
    private File tmp;

    private PatchGenerator(File patchConfig, File oldRoot, File newRoot, File patchFile) {
        this.patchConfigFile = patchConfig;
        this.oldRoot = oldRoot;
        this.newRoot = newRoot;
        this.patchFile = patchFile;
    }

    private void process() throws IOException, XMLStreamException {

        try {
            PatchConfig patchConfig = parsePatchConfig();

            this.oldStructure = patchConfig.getOriginalDistributionStructure();
            this.newStructure = patchConfig.getUpdatedDistributionStructure();

            Set<String> required = new TreeSet<String>();
            if (newRoot == null) {
                newRoot = findDefaultNewRoot();
                if (newRoot == null) {
                    required.add("--updated-dist");
                }
            }
            if (oldRoot == null) {
                oldRoot = findDefaultOldRoot(patchConfig);
                if (oldRoot == null) {
                    required.add("--applies-to-dist");
                }
            }
            if (patchFile == null) {
                if (newRoot != null) {
                    patchFile = new File(newRoot, "patch-" + System.currentTimeMillis() + ".par");
                } else {
                    required.add("--output-file");
                }
            }
            if (!required.isEmpty()) {
                System.err.printf(PatchMessages.MESSAGES.missingRequiredArgs(required));
                usage();
                return;
            }

            createTempStructure("test");

            // Create the distributions
            final Distribution base = Distribution.create(oldRoot);
            final Distribution updated = Distribution.create(newRoot);

            // Build the patch metadata
            final PatchBuilderWrapper wrapper = new PatchBuilderWrapper() {
                @Override
                PatchElementBuilder modifyLayer(String name, boolean addOn) {
                    return addElement(randomUUID().toString(), name, addOn);
                }
            };

            // Add some random information until we have a patch-config replacement
            wrapper.setPatchId(randomUUID().toString());
            wrapper.setDescription("Test patch");
            wrapper.setIdentity(Version.AS_RELEASE_CODENAME, Version.AS_VERSION);
            wrapper.setNoUpgrade();

            // Create the resulting patch
            final Patch patch = wrapper.compare(base, updated);

            // Copy the contents to the temp dir structure
            PatchContentWriter.process(tmp, newRoot, patch);

            // Create the patch
            ZipUtils.zip(tmp, patchFile);

        } finally {
            IoUtils.recursiveDelete(tmp);
        }

    }

    private PatchConfig parsePatchConfig() throws FileNotFoundException, XMLStreamException {
        if (true) {
            final PatchConfigBuilder builder = new PatchConfigBuilder();
            return builder.build();
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(patchConfigFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            return PatchConfigXml.parse(bis);
        } finally {
            IoUtils.safeClose(fis);
        }
    }

    private File findDefaultNewRoot() {
        File root = new File(System.getProperty("user.dir"));
        if (root.getName().equals("bin")) {
            root = root.getParentFile();
        }
        // See if this root has a MODULE_ROOT child and an IGNORED child; if so it looks like an AS
        boolean[] modIgnored = new boolean[2];
        File[] children = root.listFiles();
        if (children != null) {
            for (File child : children) {
                if (processModIgnored(child, newStructure, modIgnored)) {
                    break;
                }
            }
        }

        return (modIgnored[0] && modIgnored[1]) ? root : null;
    }

    private static boolean processModIgnored(File file, DistributionStructure structure, boolean[] modIgnored) {
        // TODO does that actually make any sense?
        return modIgnored[0] && modIgnored[1];
    }

    private File findDefaultOldRoot(PatchConfig patchConfig) {
        if (newRoot == null) {
            return null;
        }
        File rootParent = newRoot.getParentFile();
        if (rootParent == null) {
            return null;
        }

        for (String appliesTo : patchConfig.getAppliesTo()) {
            File root = new File(rootParent, appliesTo);
            if (!root.exists()) {
                continue;
            }

            // See if this root has a MODULE_ROOT child and an IGNORED child; if so it looks like an AS
            boolean[] modIgnored = new boolean[2];
//            DistributionContentItem newDistRoot = DistributionContentItem.createDistributionRoot();
//            File[] children = root.listFiles();
//            if (children != null) {
//                for (File child : children) {
//                    if (processModIgnored(child, newDistRoot, oldStructure, modIgnored)) {
//                        break;
//                    }
//                }
//            }

            if (modIgnored[0] && modIgnored[1]) {
                return root;
            }
        }
        return null;
    }

    private void createTempStructure(String patchId) {

        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        int count = 0;
        while (tmp == null || tmp.exists()) {
            count++;
            tmp = new File(tmpDir, "jboss-as-patch-" + patchId + "-" + count);
        }
        if (!tmp.mkdirs()) {
            throw new RuntimeException("Cannot create tmp dir for patch creation at " + tmp);
        }
        tmp.deleteOnExit();
        File metaInf = new File(tmp, "META-INF");
        metaInf.mkdir();
        metaInf.deleteOnExit();
        File misc = new File(tmp, "misc");
        misc.mkdir();
        misc.deleteOnExit();
    }

    private static PatchGenerator parse(String[] args) {

        File patchConfig = null;
        File oldFile = null;
        File newFile = null;
        File patchFile = null;

        final int argsLength = args.length;
        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];
            try {
                if ("--version".equals(arg) || "-v".equals(arg)
                        || "-version".equals(arg) || "-V".equals(arg)) {
                    ProductConfig productConfig = new ProductConfig(Module.getBootModuleLoader(), SecurityActions.getSystemProperty("jboss.home.dir"), Collections.emptyMap());
                    System.out.println(productConfig.getPrettyVersionString());
                    return null;
                } else if ("--help".equals(arg) || "-h".equals(arg) || "-H".equals(arg)) {
                    usage();
                    return null;
                } else if (arg.startsWith("--applies-to-dist=")) {
                    String val = arg.substring("--applies-to-dist=".length());
                    oldFile = new File(val);
                    if (!oldFile.exists()) {
                        System.err.printf(PatchMessages.MESSAGES.fileDoesNotExist(arg));
                        usage();
                        return null;
                    } else if (!oldFile.isDirectory()) {
                        System.err.printf(PatchMessages.MESSAGES.fileIsNotADirectory(arg));
                        usage();
                        return null;
                    }
                } else if (arg.startsWith("--updated-dist=")) {
                    String val = arg.substring("--updated-dist=".length());
                    newFile = new File(val);
                    if (!newFile.exists()) {
                        System.err.printf(PatchMessages.MESSAGES.fileDoesNotExist(arg));
                        usage();
                        return null;
                    } else if (!newFile.isDirectory()) {
                        System.err.printf(PatchMessages.MESSAGES.fileIsNotADirectory(arg));
                        usage();
                        return null;
                    }
                } else if (arg.startsWith("--patch-config=")) {
                    String val = arg.substring("--patch-config=".length());
                    patchConfig = new File(val);
                    if (!patchConfig.exists()) {
                        System.err.printf(PatchMessages.MESSAGES.fileDoesNotExist(arg));
                        usage();
                        return null;
                    } else if (patchConfig.isDirectory()) {
                        System.err.printf(PatchMessages.MESSAGES.fileIsADirectory(arg));
                        usage();
                        return null;
                    }
                } else if (arg.startsWith("--output-file=")) {
                    String val = arg.substring("--output-file=".length());
                    patchFile = new File(val);
                    if (patchFile.exists() && patchFile.isDirectory()) {
                        System.err.printf(PatchMessages.MESSAGES.fileIsADirectory(arg));
                        usage();
                        return null;
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.printf(PatchMessages.MESSAGES.argumentExpected(arg));
                usage();
                return null;
            }
        }

//        if (patchConfig == null) {
//            System.err.printf(PatchMessages.MESSAGES.missingRequiredArgs(Collections.singleton("--patch-config")));
//            usage();
//            return null;
//        }

        return new PatchGenerator(patchConfig, oldFile, newFile, patchFile);
    }

    private static void usage() {

        Usage usage = new Usage();

        usage.addArguments("--applies-to-dist=<file>");
        usage.addInstruction(PatchMessages.MESSAGES.argAppliesToDist());

        usage.addArguments("-h", "--help");
        usage.addInstruction(PatchMessages.MESSAGES.argHelp());

        usage.addArguments("--output-file=<file>");
        usage.addInstruction(PatchMessages.MESSAGES.argOutputFile());

//        usage.addArguments("--patch-config=<file>");
//        usage.addInstruction(PatchMessages.MESSAGES.argPatchConfig());

        usage.addArguments("--updated-dist=<file>");
        usage.addInstruction(PatchMessages.MESSAGES.argUpdatedDist());

        usage.addArguments("-v", "--version");
        usage.addInstruction(PatchMessages.MESSAGES.argVersion());

        String headline = usage.getDefaultUsageHeadline("patch-gen");
        System.out.print(usage.usage(headline));

    }
}
