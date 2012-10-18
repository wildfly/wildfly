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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.metadata.BundleItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModificationType;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.metadata.PatchXml;
import org.jboss.as.version.ProductConfig;
import org.jboss.as.version.Usage;
import org.jboss.modules.Module;

/**
 * Generates a patch archive.
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
    private final File oldRoot;
    private final File newRoot;
    private DistributionStructure oldStructure;
    private DistributionStructure newStructure;
    private final File patchFile;
    private final Map<DistributionContentItem, ContentModification> bundleAdds = new TreeMap<DistributionContentItem, ContentModification>();
    private final Map<DistributionContentItem, ContentModification> bundleUpdates = new TreeMap<DistributionContentItem, ContentModification>();
    private final Map<DistributionContentItem, ContentModification> bundleRemoves = new TreeMap<DistributionContentItem, ContentModification>();
    private final Map<DistributionContentItem, ContentModification> moduleAdds = new TreeMap<DistributionContentItem, ContentModification>();
    private final Map<DistributionContentItem, ContentModification> moduleUpdates = new TreeMap<DistributionContentItem, ContentModification>();
    private final Map<DistributionContentItem, ContentModification> moduleRemoves = new TreeMap<DistributionContentItem, ContentModification>();
    private final Map<DistributionContentItem, ContentModification> miscAdds = new TreeMap<DistributionContentItem, ContentModification>();
    private final Map<DistributionContentItem, ContentModification> miscUpdates = new TreeMap<DistributionContentItem, ContentModification>();
    private final Map<DistributionContentItem, ContentModification> miscRemoves = new TreeMap<DistributionContentItem, ContentModification>();
    private final MessageDigest messageDigest;
    private File tmp;

    private PatchGenerator(File patchConfig, File oldRoot, File newRoot, File patchFile) {
        this.patchConfigFile = patchConfig;
        this.oldRoot = oldRoot;
        this.newRoot = newRoot;
        this.patchFile = patchFile;

        try {
            this.messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot obtain SHA-1 MessageDigest");
        }
    }

    private void process() throws IOException, XMLStreamException {

        try {
            PatchConfig patchConfig = parsePatchConfig();

            createTempStructure(patchConfig);

            this.oldStructure = DistributionStructure.Factory.create(patchConfig.getAppliesTo().iterator().next());
            this.newStructure = DistributionStructure.Factory.create(patchConfig.getResultingVersion());

            if (patchConfig.isGenerateByDiff()) {
                analyzeDifferences(patchConfig);
            }

            try {
                preparePatchXml(patchConfig);
            } catch (IOException e) {
                throw new RuntimeException("Failed creating patch.xml file", e);
            }

            copyContent();

            ZipUtils.zip(tmp, patchFile);

        } finally {
            cleanFile(tmp);
        }

    }

    private PatchConfig parsePatchConfig() throws FileNotFoundException, XMLStreamException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(patchConfigFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            return PatchConfigXml.parse(bis);
        } finally {
            PatchUtils.safeClose(fis);
        }
    }

    private void createTempStructure(PatchConfig patchConfig) {

        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        int count = 0;
        while (tmp == null || tmp.exists()) {
            count++;
            tmp = new File(tmpDir, "jboss-as-patch-" + patchConfig.getPatchId() + "-" + count);
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

    private void analyzeDifferences(PatchConfig patchConfig) throws IOException {

        Set<DistributionContentItem> oldVisited = new HashSet<DistributionContentItem>();
        DistributionContentItem newDistRoot = DistributionContentItem.createDistributionRoot();
        File[] children = newRoot.listFiles();
        if (children != null) {
            for (File child : children) {
                processNewVersionFile(child, newDistRoot, patchConfig,  oldVisited);
            }
        }

        children = oldRoot.listFiles();
        if (children != null) {
            DistributionContentItem oldDistRoot = DistributionContentItem.createDistributionRoot();
            for (File child : children) {
                processOldVersionFile(child, oldDistRoot, patchConfig, oldVisited);
            }
        }

    }

    private void processNewVersionFile(File file, DistributionContentItem parent, PatchConfig patchConfig, Set<DistributionContentItem> oldVisited) throws IOException {
        DistributionContentItem itemPath = newStructure.getContentItem(file, parent);
        DistributionContentItem.Type type = itemPath.getType();
        File oldFile = null;
        DistributionContentItem oldItemPath = newStructure.getPreviousVersionPath(itemPath, oldStructure);
        if (oldItemPath != null) {
            oldVisited.add(oldItemPath);
            oldFile = oldItemPath.getFile(oldRoot);
        }
        boolean recurse = type.getHasRelevantChildren();
        switch (type) {
            case BUNDLE_PARENT:
                // just recurse
                break;
            case BUNDLE_ROOT:
                if (oldFile == null || !oldFile.exists()) {
                    recordBundleAdd(itemPath);
                    recurse = false;
                } // else just recurse
                break;
            case BUNDLE_CONTENT:
                if (oldFile == null || !oldFile.exists()) {
                    recordBundleUpdate(itemPath);
                    recurse = false;
                } else {
                    byte[] newHash = getHash(itemPath, newRoot);
                    byte[] oldHash = getHash(oldFile);
                    if (!Arrays.equals(newHash, oldHash)) {
                        recordBundleUpdate(itemPath);
                        recurse = false;
                    } // else no change
                }
                break;
            case MODULE_PARENT:
                // just recurse
                break;
            case MODULE_ROOT:
                if (oldFile == null || !oldFile.exists()) {
                    recordModuleAdd(itemPath);
                    recurse = false;
                } // else just recurse
                break;
            case MODULE_CONTENT:
                if (oldFile == null || !oldFile.exists()) {
                    recordModuleUpdateViaContentChange(itemPath);
                    recurse = false;
                } else {
                    DistributionContentItem moduleRoot = getModuleRoot(itemPath);
                    if (!moduleUpdates.containsKey(moduleRoot)) {
                        byte[] newHash = getHash(itemPath, newRoot);
                        byte[] oldHash = getHash(oldFile);
                        if (!Arrays.equals(newHash, oldHash)) {
                            recordModuleUpdateViaContentChange(moduleRoot);
                            recurse = false;
                        } // else no change
                    }
                }
                break;
            case MISC:
                if (oldFile == null || !oldFile.exists()) {
                    recordMiscFileAdd(itemPath);
                } else if (shouldCompareContents(file, oldFile)) {
                    byte[] newHash = getHash(itemPath, newRoot);
                    byte[] oldHash = getHash(oldFile);
                    if (!Arrays.equals(newHash, oldHash)) {
                        recordMiscFileUpdate(itemPath, patchConfig, newHash, oldHash);
                    } // else no change
                }
                break;
            case IGNORED:
                // ok, we'll ignore it!
                break;
            case DISTRIBUTION_ROOT:
            default:
                throw new IllegalStateException();
        }

        if (recurse && file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    processNewVersionFile(child, itemPath, patchConfig, oldVisited);
                }
            }
        }
    }

    private boolean shouldCompareContents(File newFile, File oldFile) {
        return !newFile.isDirectory() || !oldFile.isDirectory();
    }

    private void processOldVersionFile(File file, DistributionContentItem parent, PatchConfig patchConfig,
                                       Set<DistributionContentItem> oldVisited) throws IOException {

        DistributionContentItem itemPath = oldStructure.getContentItem(file, parent);
        DistributionContentItem.Type type = itemPath.getType();
        boolean recurse = type.getHasRelevantChildren();

        if (!oldVisited.contains(itemPath)) {
            switch (type) {
                case BUNDLE_PARENT:
                    // just recurse;
                    break;
                case BUNDLE_ROOT:
                    recordBundleRemove(itemPath);
                    recurse = false;
                    break;
                case BUNDLE_CONTENT:
                    // A piece of bundle content exists in the old version and not in the new. This is a bundle update
                    DistributionContentItem newItemPath = newStructure.getCurrentVersionPath(itemPath, oldStructure);
                    recordBundleUpdate(newItemPath);
                    recurse = false;
                    break;
                case MODULE_PARENT:
                    // just recurse
                    break;
                case MODULE_ROOT:
                    recordModuleRemove(itemPath);
                    recurse = false;
                    break;
                case MODULE_CONTENT:
                    recordModuleUpdateViaContentRemove(itemPath);
                    recurse = false;
                    break;
                case MISC:
                    recordMiscFileRemove(itemPath, patchConfig,  getHash(file));
                    break;
                case IGNORED:
                    // ok, we'll ignore it!
                    break;
                case DISTRIBUTION_ROOT:
                default:
                    throw new IllegalStateException();
            }
        } // else we already looked at this path when processing the new version files

        if (recurse && file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    processOldVersionFile(child, itemPath, patchConfig, oldVisited);
                }
            }
        }
    }

    private void recordBundleAdd(DistributionContentItem itemPath) throws IOException {
        DistributionContentItem bundleRoot = getBundleRoot(itemPath);
        File moduleRootFile = bundleRoot.getFile(newRoot);
        byte[] moduleHash = getHash(moduleRootFile);
        BundleItem bi = new BundleItem(newStructure.getBundleName(bundleRoot), newStructure.getBundleSlot(bundleRoot), moduleHash);
        ContentModification cm = new ContentModification(bi, PatchUtils.NO_CONTENT, ModificationType.ADD);
        bundleAdds.put(bundleRoot, cm);
    }

    private void recordBundleUpdate(DistributionContentItem itemPath) throws IOException {

        DistributionContentItem bundleRoot = getBundleRoot(itemPath);
        File newBundleRootFile = bundleRoot.getFile(newRoot);
        byte[] newItemHash = getHash(newBundleRootFile);
        BundleItem bi = new BundleItem(newStructure.getBundleName(bundleRoot), newStructure.getBundleSlot(bundleRoot), newItemHash);
        DistributionContentItem oldBundleRoot = newStructure.getPreviousVersionPath(bundleRoot, oldStructure);
        File oldModuleRootFile = oldBundleRoot.getFile(oldRoot);
        byte[] oldItemHash = getHash(oldModuleRootFile);
        ContentModification cm = new ContentModification(bi, oldItemHash, ModificationType.MODIFY);
        bundleUpdates.put(bundleRoot, cm);
    }

    private void recordBundleRemove(DistributionContentItem oldItemPath) throws IOException {
        DistributionContentItem bundleRoot = getBundleRoot(oldItemPath);
        BundleItem bi = new BundleItem(oldStructure.getBundleName(bundleRoot), oldStructure.getBundleSlot(bundleRoot), PatchUtils.NO_CONTENT);
        File oldDir = bundleRoot.getFile(oldRoot);
        byte[] oldItemHash = getHash(oldDir);
        ContentModification cm = new ContentModification(bi, oldItemHash, ModificationType.REMOVE);
        bundleRemoves.put(bundleRoot, cm);
    }

    private void recordModuleAdd(DistributionContentItem itemPath) throws IOException {
        DistributionContentItem moduleRoot = getModuleRoot(itemPath);
        File moduleRootFile = moduleRoot.getFile(newRoot);
        byte[] moduleHash = getHash(moduleRootFile);
        ModuleItem mi = new ModuleItem(newStructure.getModuleName(moduleRoot), newStructure.getModuleSlot(moduleRoot), moduleHash);
        ContentModification cm = new ContentModification(mi, PatchUtils.NO_CONTENT, ModificationType.ADD);
        moduleAdds.put(moduleRoot, cm);
    }

    private void recordModuleUpdateViaContentChange(DistributionContentItem itemPath) throws IOException {
        DistributionContentItem moduleRoot = getModuleRoot(itemPath);
        File newModuleRootFile = moduleRoot.getFile(newRoot);
        byte[] newItemHash = getHash(newModuleRootFile);

        ModuleItem mi = new ModuleItem(newStructure.getModuleName(moduleRoot), newStructure.getModuleSlot(moduleRoot), newItemHash);
        DistributionContentItem oldModuleRoot = newStructure.getPreviousVersionPath(moduleRoot, oldStructure);
        File oldModuleRootFile = oldModuleRoot.getFile(oldRoot);
        byte[] oldItemHash = getHash(oldModuleRootFile);
        ContentModification cm = new ContentModification(mi, oldItemHash, ModificationType.MODIFY);
        moduleUpdates.put(moduleRoot, cm);
    }

    private void recordModuleUpdateViaContentRemove(DistributionContentItem removedPath) throws IOException {
        DistributionContentItem oldModuleRoot = getModuleRoot(removedPath);
        DistributionContentItem newModuleRoot = newStructure.getCurrentVersionPath(oldModuleRoot, oldStructure);
        if (!moduleUpdates.containsKey(newModuleRoot)) {
            File newModuleRootFile = newModuleRoot.getFile(oldRoot);
            byte[] newItemHash = getHash(newModuleRootFile);

            ModuleItem mi = new ModuleItem(newStructure.getModuleName(newModuleRoot), newStructure.getModuleSlot(newModuleRoot), newItemHash);
            File oldModuleRootFile = oldModuleRoot.getFile(oldRoot);
            byte[] oldItemHash = getHash(oldModuleRootFile);
            ContentModification cm = new ContentModification(mi, oldItemHash, ModificationType.MODIFY);
            moduleUpdates.put(newModuleRoot, cm);
        }
    }

    private void recordModuleRemove(DistributionContentItem oldModuleRoot) throws IOException {
        ModuleItem mi = new ModuleItem(oldStructure.getModuleName(oldModuleRoot), oldStructure.getModuleSlot(oldModuleRoot), PatchUtils.NO_CONTENT);
        File oldDir = oldModuleRoot.getFile(oldRoot);
        byte[] oldItemHash = getHash(oldDir);
        ContentModification cm = new ContentModification(mi, oldItemHash, ModificationType.REMOVE);
        moduleRemoves.put(oldModuleRoot, cm);
    }

    private void recordMiscFileAdd(DistributionContentItem itemPath) throws IOException {
        File added = itemPath.getFile(newRoot);
        byte[] hash = getHash(added);
        MiscContentItem mci = new MiscContentItem(itemPath.getName(), itemPath.getParent().getPathAsList(), hash, itemPath.isDirectory());
        ContentModification cm = new ContentModification(mci, PatchUtils.NO_CONTENT, ModificationType.ADD);
        miscAdds.put(itemPath, cm);
    }

    private void recordMiscFileUpdate(DistributionContentItem itemPath, PatchConfig config, byte[] newItemHash, byte[] oldItemHash) throws IOException {
        List<String> list = itemPath.getParent().getPathAsList();
        String[] array = list.toArray(new String[list.size()]);
        MiscContentItem mci = new MiscContentItem(itemPath.getName(), array, newItemHash, itemPath.isDirectory(), config.getInRuntimeUseItems().contains(itemPath));
        ContentModification cm = new ContentModification(mci, oldItemHash, ModificationType.MODIFY);
        miscUpdates.put(itemPath, cm);
    }

    private void recordMiscFileRemove(DistributionContentItem oldItemPath, PatchConfig config, byte[] oldItemHash) {
        List<String> list = oldItemPath.getParent().getPathAsList();
        String[] array = list.toArray(new String[list.size()]);
        MiscContentItem mci = new MiscContentItem(oldItemPath.getName(), array, oldItemHash, oldItemPath.isDirectory(), config.getInRuntimeUseItems().contains(oldItemPath));
        ContentModification cm = new ContentModification(mci, oldItemHash, ModificationType.REMOVE);
        miscRemoves.put(oldItemPath, cm);
    }

    private byte[] getHash(DistributionContentItem itemPath, File rootFile) throws IOException {
        File file = itemPath.getFile(rootFile);
        return getHash(file);
    }

    private byte[] getHash(File file) throws IOException {
//        try {
            return PatchUtils.hashFile(file, messageDigest);
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to hash file " + file.getAbsolutePath(), e);
//        }
    }

    private static DistributionContentItem getModuleRoot(DistributionContentItem contentItem) {
        DistributionContentItem item = contentItem;
        while (item.getType() != DistributionContentItem.Type.MODULE_ROOT) {
            item = item.getParent();
        }
        return item;
    }

    private static DistributionContentItem getBundleRoot(DistributionContentItem contentItem) {
        DistributionContentItem item = contentItem;
        while (item.getType() != DistributionContentItem.Type.BUNDLE_ROOT) {
            item = item.getParent();
        }
        return item;
    }

    private void preparePatchXml(PatchConfig patchConfig) throws IOException, XMLStreamException {

        File patchXml = new File(tmp, "patch.xml");
        FileOutputStream fos = null;
        try {
            PatchBuilder pb = patchConfig.toPatchBuilder();

            for (ContentModification cm : moduleAdds.values()) {
                pb.addContentModification(cm);
            }

            for (ContentModification cm : moduleUpdates.values()) {
                pb.addContentModification(cm);
            }

            for (ContentModification cm : moduleRemoves.values()) {
                pb.addContentModification(cm);
            }

            for (ContentModification cm : bundleAdds.values()) {
                pb.addContentModification(cm);
            }

            for (ContentModification cm : bundleUpdates.values()) {
                pb.addContentModification(cm);
            }

            for (ContentModification cm : bundleRemoves.values()) {
                pb.addContentModification(cm);
            }

            for (ContentModification cm : miscAdds.values()) {
                pb.addContentModification(cm);
            }

            for (ContentModification cm : miscUpdates.values()) {
                pb.addContentModification(cm);
            }

            for (ContentModification cm : miscRemoves.values()) {
                pb.addContentModification(cm);
            }

            patchXml.createNewFile();
            fos = new FileOutputStream(patchXml);
            PatchXml.marshal(fos, pb.build());

        } finally {
            PatchUtils.safeClose(fos);
        }

        FileReader reader = new FileReader(patchXml);
        try {
            BufferedReader br = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } finally {
            PatchUtils.safeClose(reader);
        }
    }

    private void copyContent() {

        File baseFile = tmp;
        for (DistributionContentItem item : moduleAdds.keySet()) {
            copyFile(item, baseFile);
        }

        for (DistributionContentItem item : moduleUpdates.keySet()) {
            copyFile(item, baseFile);
        }

        for (DistributionContentItem item : bundleAdds.keySet()) {
            copyFile(item, baseFile);
        }

        for (DistributionContentItem item : bundleUpdates.keySet()) {
            copyFile(item, baseFile);
        }

        baseFile = new File(tmp, "misc");

        for (DistributionContentItem item : miscAdds.keySet()) {
            copyFile(item, baseFile);
        }

        for (DistributionContentItem item : miscUpdates.keySet()) {
            copyFile(item, baseFile);
        }
    }

    private void copyFile(DistributionContentItem targetItem, File targetBaseDir) {
        File targetFile = targetItem.getFile(targetBaseDir);
        File sourceFile = targetItem.getFile(newRoot);
        try {
            PatchUtils.copyFile(sourceFile, targetFile);
        } catch (IOException e) {
            throw new RuntimeException("Cannot copy " + sourceFile + " to " + targetFile, e);
        }
    }

    private void cleanFile(File file) {
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        cleanFile(child);
                    }
                }
            }

            System.out.println("Cleaned " + file + " -- " + file.delete());
        }
    }

    private static PatchGenerator parse(String[] args) {

        File patchConfig = null;
        File oldFile = null;
        File newFile = null;
        File patchFile = null;

        Set<String> required = new HashSet<String>();
        Collections.addAll(required, "--applies-to-dist", "--output-file", "--patch-config", "--updated-dist");
        final int argsLength = args.length;
        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];
            try {
                if ("--version".equals(arg) || "-v".equals(arg)
                        || "-version".equals(arg) || "-V".equals(arg)) {
                    ProductConfig productConfig = new ProductConfig(Module.getBootModuleLoader(), SecurityActions.getSystemProperty("jboss.home.dir"));
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
                    required.remove("--applies-to-dist");
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
                    required.remove("--updated-dist");
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
                    required.remove("--patch-config");
                } else if (arg.startsWith("--output-file=")) {
                    String val = arg.substring("--output-file=".length());
                    patchFile = new File(val);
                    if (patchFile.exists() && patchFile.isDirectory()) {
                        System.err.printf(PatchMessages.MESSAGES.fileIsADirectory(arg));
                        usage();
                        return null;
                    }
                    required.remove("--output-file");
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.printf(PatchMessages.MESSAGES.argumentExpected(arg));
                usage();
                return null;
            }
        }

        if (!required.isEmpty()) {
            System.err.printf(PatchMessages.MESSAGES.missingRequiredArgs(required));
            usage();
            return null;
        }

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

        usage.addArguments("--patch-config=<file>");
        usage.addInstruction(PatchMessages.MESSAGES.argPatchConfig());

        usage.addArguments("--updated-dist=<file>");
        usage.addInstruction(PatchMessages.MESSAGES.argUpdatedDist());

        usage.addArguments("-v", "--version");
        usage.addInstruction(PatchMessages.MESSAGES.argVersion());

        String headline = PatchMessages.MESSAGES.patchGeneratorUsageHeadline("TODO"); // TODO
        System.out.print(usage.usage(headline));

    }
}
