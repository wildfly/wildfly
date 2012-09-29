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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * TODO class javadoc.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class PatchGenerator {

    public static void main(String[] args) {
        PatchGenerator patchGenerator = parse(args);
        patchGenerator.process();
    }

    private final File oldRoot;
    private final File newRoot;
    private final DistributionStructure oldStructure;
    private final DistributionStructure newStructure;
    private final File patchFile;
    private final Set<DistributionContentItem> bundleAdds = new TreeSet<DistributionContentItem>();
    private final Set<DistributionContentItem> bundleUpdates = new TreeSet<DistributionContentItem>();
    private final Set<DistributionContentItem> bundleRemoves = new TreeSet<DistributionContentItem>();
    private final Set<DistributionContentItem> moduleAdds = new TreeSet<DistributionContentItem>();
    private final Map<DistributionContentItem, HashedContentItem> moduleUpdates = new TreeMap<DistributionContentItem, HashedContentItem>();
    private final Set<HashedContentItem> moduleRemoves = new TreeSet<HashedContentItem>();
    private final Set<DistributionContentItem> miscAdds = new TreeSet<DistributionContentItem>();
    private final Map<DistributionContentItem, HashedContentItem> miscUpdates = new TreeMap<DistributionContentItem, HashedContentItem>();
    private final Set<HashedContentItem> miscRemoves = new TreeSet<HashedContentItem>();
    private final MessageDigest messageDigest;
    private File tmp;

    private PatchGenerator(String oldVersion, File oldRoot, String newVersion, File newRoot, File patchFile) {
        this.oldRoot = oldRoot;
        this.newRoot = newRoot;
        this.patchFile = patchFile;

        try {
            this.messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot obtain SHA-1 MessageDigest");
        }

        this.oldStructure = DistributionStructure.Factory.create(oldVersion);
        this.newStructure = DistributionStructure.Factory.create(newVersion);
    }

    private void process() {

        try {
            createTempStructure();

            analyzeDifferences();

            try {
                preparePatchXml();
            } catch (IOException e) {
                throw new RuntimeException("Failed creating patch.xml file", e);
            }

            copyContent();

            preparePatchFile();

        } finally {
            cleanFile(tmp);
        }

    }

    private void createTempStructure() {

        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        int count = 0;
        while (tmp == null || tmp.exists()) {
            count++;
            tmp = new File(tmpDir, "jboss-as-patch-" + count);
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

    private void analyzeDifferences() {

        Set<DistributionContentItem> oldVisited = new HashSet<DistributionContentItem>();
        DistributionContentItem newDistRoot = DistributionContentItem.createDistributionRoot();
        File[] children = newRoot.listFiles();
        if (children != null) {
            for (File child : children) {
                processNewVersionFile(child, newDistRoot, oldVisited);
            }
        }

        children = oldRoot.listFiles();
        if (children != null) {
            DistributionContentItem oldDistRoot = DistributionContentItem.createDistributionRoot();
            for (File child : children) {
                processOldVersionFile(child, oldDistRoot, oldVisited);
            }
        }

    }

    private void processNewVersionFile(File file, DistributionContentItem parent, Set<DistributionContentItem> oldVisited) {
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
                } else {
                    byte[] newHash = getHash(itemPath, newRoot);
                    byte[] oldHash = getHash(oldFile);
                    if (!Arrays.equals(newHash, oldHash)) {
                        recordMiscFileUpdate(itemPath, oldItemPath, oldHash);
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
                    processNewVersionFile(child, itemPath, oldVisited);
                }
            }
        }
    }

    private void processOldVersionFile(File file, DistributionContentItem parent, Set<DistributionContentItem> oldVisited) {

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
                    recordMiscFileRemove(itemPath, getHash(file));
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
                    processOldVersionFile(child, itemPath, oldVisited);
                }
            }
        }
    }

    private void recordBundleAdd(DistributionContentItem itemPath) {
        DistributionContentItem item = itemPath;
        while (item.getType() != DistributionContentItem.Type.BUNDLE_ROOT) {
            item = item.getParent();
        }
        bundleAdds.add(item);
    }

    private void recordBundleUpdate(DistributionContentItem itemPath) {

        DistributionContentItem item = itemPath;
        while (item.getType() != DistributionContentItem.Type.BUNDLE_ROOT) {
            item = item.getParent();
        }
        bundleUpdates.add(item);
    }

    private void recordBundleRemove(DistributionContentItem oldItemPath) {
        DistributionContentItem item = oldItemPath;
        while (item.getType() != DistributionContentItem.Type.BUNDLE_ROOT) {
            item = item.getParent();
        }
        bundleRemoves.add(item);
    }

    private void recordModuleAdd(DistributionContentItem itemPath) {
        DistributionContentItem moduleRoot = getModuleRoot(itemPath);
        moduleAdds.add(moduleRoot);
    }

    private void recordModuleUpdateViaContentChange(DistributionContentItem itemPath) {
        DistributionContentItem moduleRoot = getModuleRoot(itemPath);
        DistributionContentItem oldModuleRoot = newStructure.getPreviousVersionPath(moduleRoot, oldStructure);
        File oldModuleRootFile = oldModuleRoot.getFile(oldRoot);
        byte[] oldItemHash = getHash(oldModuleRootFile);
        moduleUpdates.put(moduleRoot, new HashedContentItem(oldModuleRoot, oldItemHash));
    }

    private void recordModuleUpdateViaContentRemove(DistributionContentItem removedPath) {
        DistributionContentItem oldModuleRoot = getModuleRoot(removedPath);
        DistributionContentItem newModuleRoot = newStructure.getCurrentVersionPath(oldModuleRoot, oldStructure);
        if (!moduleUpdates.containsKey(newModuleRoot)) {
            File oldModuleRootFile = oldModuleRoot.getFile(oldRoot);
            byte[] oldItemHash = getHash(oldModuleRootFile);
            moduleUpdates.put(newModuleRoot, new HashedContentItem(oldModuleRoot, oldItemHash));
        }
    }

    private void recordModuleRemove(DistributionContentItem oldModuleRoot) {
        File oldDir = oldModuleRoot.getFile(oldRoot);
        byte[] oldItemHash = getHash(oldDir);
        moduleRemoves.add(new HashedContentItem(oldModuleRoot, oldItemHash));
    }

    private void recordMiscFileAdd(DistributionContentItem itemPath) {
        miscAdds.add(itemPath);
    }

    private void recordMiscFileUpdate(DistributionContentItem itemPath, DistributionContentItem oldItemPath, byte[] oldItemHash) {
        miscUpdates.put(itemPath, new HashedContentItem(oldItemPath, oldItemHash));
    }

    private void recordMiscFileRemove(DistributionContentItem oldItemPath, byte[] oldItemHash) {
        miscRemoves.add(new HashedContentItem(oldItemPath, oldItemHash));
    }

    private byte[] getHash(DistributionContentItem itemPath, File rootFile) {
        File file = itemPath.getFile(rootFile);
        return getHash(file);
    }

    private byte[] getHash(File file) {
        try {
            return PatchUtils.hashFile(file, messageDigest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash file " + file.getAbsolutePath(), e);
        }
    }

    private static DistributionContentItem getModuleRoot(DistributionContentItem contentItem) {
        DistributionContentItem item = contentItem;
        while (item.getType() != DistributionContentItem.Type.MODULE_ROOT) {
            item = item.getParent();
        }
        return item;
    }

    private void preparePatchXml() throws IOException {

        // TODO actually implement

        File patchXml = new File(tmp, "patch.xml");
        patchXml.createNewFile();
        FileOutputStream fos = new FileOutputStream(patchXml);
        try {
            PrintWriter pw = new PrintWriter(fos);
            pw.println("Added modules:\n");

            for (DistributionContentItem item : moduleAdds) {
                pw.println("name: " + newStructure.getModuleName(item) + "  slot: " + newStructure.getModuleSlot(item));
            }

            pw.println("\nUpdated modules:\n");

            for (Map.Entry<DistributionContentItem, HashedContentItem> entry : moduleUpdates.entrySet()) {
                DistributionContentItem item = entry.getKey();
                HashedContentItem oldItem = entry.getValue();
                pw.println("name: " + newStructure.getModuleName(item) + "  slot: " + newStructure.getModuleSlot(item)
                        + "  old path: " + oldItem.item.getPath() + "  hash: " + PatchUtils.bytesToHexString(oldItem.hash));
            }

            pw.println("\nRemoved modules:\n");

            for (HashedContentItem item : moduleRemoves) {
                pw.print("name: " + newStructure.getModuleName(item.item) + "  slot: " + newStructure.getModuleSlot(item.item));
                if (item.hash != null) {
                    pw.print("  hash: " + PatchUtils.bytesToHexString(item.hash));
                }
                pw.println();
            }

            pw.println("\nAdded bundles:\n");

            for (DistributionContentItem item : bundleAdds) {
                pw.println("name: " + newStructure.getBundleName(item) );
            }

            System.out.println("\nUpdated bundles:\n");

            for (DistributionContentItem item : bundleUpdates) {
                pw.println("name: " + newStructure.getBundleName(item) );
            }

            pw.println("\nRemoved bundles:\n");

            for (DistributionContentItem item : bundleRemoves) {
                pw.println("name: " + newStructure.getBundleName(item) );
            }

            pw.println("\nAdded misc files:\n");

            for (DistributionContentItem item : miscAdds) {
                pw.println("path: " + item.getPath());
            }

            pw.println("\nUpdated misc files:\n");

            for (Map.Entry<DistributionContentItem, HashedContentItem> entry : miscUpdates.entrySet()) {
                HashedContentItem item = entry.getValue();
                pw.println("path: " + entry.getKey().getPath() + "  old path: " + item.item.getPath()
                        + "  hash: " + PatchUtils.bytesToHexString(item.hash));
            }

            pw.println("\nRemoved misc files:\n");

            for (HashedContentItem item : miscRemoves) {
                String hash = item.hash == null ? "<directory>" : PatchUtils.bytesToHexString(item.hash);
                pw.println("path: " + item.item.getPath() + "  hash: " + hash);
            }

            pw.println();

            pw.flush();

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
        for (DistributionContentItem item : moduleAdds) {
            copyFile(item, baseFile);
        }

        for (DistributionContentItem item : moduleUpdates.keySet()) {
            copyFile(item, baseFile);
        }

        for (DistributionContentItem item : bundleAdds) {
            copyFile(item, baseFile);
        }

        for (DistributionContentItem item : bundleUpdates) {
            copyFile(item, baseFile);
        }

        baseFile = new File(tmp, "misc");

        for (DistributionContentItem item : miscAdds) {
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

    private void preparePatchFile() {
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(patchFile));

            for (File file : tmp.listFiles()) {
                if (file.isDirectory()) {
                    addDirectoryToZip(file, file.getName(), zos);
                } else {
                    addFileToZip(file, null, zos);
                }
            }

            zos.close();

        } catch (IOException e) {
            throw new RuntimeException("Failed creating patch file " + patchFile, e);
        }

        System.out.println("\nPrepared " + patchFile.getName() + " at " + patchFile.getAbsolutePath());
    }

    private void addDirectoryToZip(File dir, String dirName, ZipOutputStream zos) throws IOException {

        ZipEntry dirEntry = new ZipEntry(dirName + "/");
        zos.putNextEntry(dirEntry);
        zos.closeEntry();

        File[] children = dir.listFiles();
        if (children != null) {
            for (File file : children) {
                if (file.isDirectory()) {
                    addDirectoryToZip(file, dirName + "/" + file.getName(), zos);
                } else {
                    addFileToZip(file, dirName, zos);
                }
            }
        }
    }

    private void addFileToZip(File file, String parent, ZipOutputStream zos) throws IOException {

        String entryName = parent == null ? file.getName() : parent + "/" + file.getName();
        zos.putNextEntry(new ZipEntry(entryName));

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

        byte[] bytesIn = new byte[4096];
        int read;
        while ((read = bis.read(bytesIn)) != -1) {
            zos.write(bytesIn, 0, read);
        }

        zos.closeEntry();
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
        // TODO actually implement
        String oldVersion = args[0];
        File oldFile = new File(args[1]);
        String newVersion = args[2];
        File newFile = new File(args[3]);
        File patchFile = new File(args[4]);
        return new PatchGenerator(oldVersion, oldFile, newVersion, newFile, patchFile);
    }

    private static class HashedContentItem implements Comparable<HashedContentItem> {
        private final DistributionContentItem item;
        private final byte[] hash;

        private HashedContentItem(DistributionContentItem item, byte[] hash) {
            this.item = item;
            this.hash = hash;
        }

        @Override
        public int compareTo(HashedContentItem o) {
            return item.compareTo(o.item);
        }

        @Override
        public int hashCode() {
            return item.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof HashedContentItem && item.equals(((HashedContentItem) obj).item);
        }
    }
}
