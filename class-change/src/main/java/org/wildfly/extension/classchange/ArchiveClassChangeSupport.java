/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.wildfly.extension.classchange;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.fakereplace.api.ChangedClass;
import org.fakereplace.api.ClassChangeAware;
import org.fakereplace.core.Fakereplace;
import org.fakereplace.replacement.AddedClass;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.classchange.logging.ClassChangeMessages;

class ArchiveClassChangeSupport extends AbstractClassChangeSupport implements DeploymentClassChangeSupport, ClassChangeAware {

    private static final String TEMP_DIR = "jboss.server.temp.dir";
    private final File replacedResourcesDir;
    private final File replacedClassesDir;
    private final VirtualFile deploymentContents;
    private final String tempDir;
    private final boolean copyToPhysicalFile;

    ArchiveClassChangeSupport(DeploymentUnit deploymentUnit) {
        super(deploymentUnit);
        this.deploymentContents = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_CONTENTS);
        PathManager pathManager = deploymentUnit.getAttachment(Attachments.PATH_MANAGER);
        tempDir = pathManager.getPathEntry(TEMP_DIR).resolvePath();
        replacedResourcesDir = new File(tempDir, deploymentUnit.getName() + "-replaced-resources");
        replacedResourcesDir.mkdirs();
        if (deploymentUnit.getName().endsWith(".war")) {
            copyToPhysicalFile = true;
            replacedClassesDir = new File(replacedResourcesDir, "WEB-INF" + File.separatorChar + "classes");
            replacedClassesDir.mkdirs();
        } else {
            copyToPhysicalFile = false;
            replacedClassesDir = replacedResourcesDir;
        }
    }

    @Override
    protected ExternalScan createScan() {
        return new ExternalScan() {

            private final Map<String, Long> known = getKnownClasses();
            private final Map<String, byte[]> changedClasses = new HashMap<>();

            @Override
            public void handleChangedExternalWebResourceFile(byte[] newVersion, String targetLocation) {
                try {
                    File file = new File(replacedResourcesDir, targetLocation);
                    file.getParentFile().mkdirs();
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        out.write(newVersion);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void handleChangedExternalClassFile(byte[] newVersion, String targetLocation) {
                changedClasses.put(targetLocation.replace(File.separatorChar, '/'), newVersion);
                try {
                    File file = new File(replacedClassesDir, targetLocation);
                    file.getParentFile().mkdirs();
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        out.write(newVersion);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void scanComplete(List<ClassDefinition> modified, List<AddedClass> addedClasses) throws IOException, ClassNotFoundException {
                //no-op, as nothing can change in the archive no further scanning is required

                for (Map.Entry<String, byte[]> entry : changedClasses.entrySet()) {

                    String className = entry.getKey().substring(0, entry.getKey().length() - CLASS.length()).replace("/", ".");
                    if (known.containsKey(entry.getKey())) {
                        try {
                            modified.add(new ClassDefinition(getClassLoader().loadClass(className), entry.getValue()));
                        } catch (ClassNotFoundException e) {
                            ClassChangeMessages.ROOT_LOGGER.failedToReplaceClassFile(className, e);
                        }
                    } else {
                        addedClasses.add(new AddedClass(className, entry.getValue(), getClassLoader()));
                    }
                }
            }
        };
    }

    @Override
    public Map<String, Long> getKnownClasses() {
        Map<String, Long> ret = super.getKnownClasses();
        getKnownClasses("", ret);
        return ret;
    }

    private void getKnownClasses(String currentPart, Map<String, Long> ret) {
        File current = new File(replacedClassesDir, currentPart);
        for (File file : current.listFiles()) {
            if (file.isDirectory()) {
                getKnownClasses(currentPart.isEmpty() ? file.getName() : currentPart + File.separatorChar + file.getName(), ret);
            } else if (file.getName().endsWith(".class")) {
                ret.put((currentPart + '/' + file.getName()).replace(File.separatorChar, '/'), file.lastModified());
            }
        }
    }

    @Override
    public Map<String, Long> getKnownWebResources() {
        Map<String, Long> ret = super.getKnownWebResources();
        getKnownWebResource("", ret);
        return ret;
    }

    private void getKnownWebResource(String currentPart, Map<String, Long> ret) {
        if (currentPart.equals("WEB-INF/classes")) {
            return;
        }
        File current = new File(replacedResourcesDir, currentPart);
        for (File file : current.listFiles()) {
            if (file.isDirectory()) {
                getKnownWebResource(currentPart.isEmpty() ? file.getName() : currentPart + File.separatorChar + file.getName(), ret);
            } else {
                ret.put(currentPart.replace(File.separatorChar, '/'), file.lastModified());
            }
        }
    }

    @Override
    public void notifyChangedClasses(Map<String, byte[]> srcFiles, Map<String, byte[]> classFiles, Map<String, byte[]> webResources) {
        try {
            Map<String, Long> known = getKnownClasses();
            Map<String, byte[]> updated = updateChangesClassesOnDisk(replacedClassesDir, replacedResourcesDir, srcFiles, classFiles, webResources);

            if (this.copyToPhysicalFile) {
                //for web deployments we also copy to the actual file
                //as web archives have been mounted with mount exploded we need
                //to do this to make sure changes are picked up
                for (Map.Entry<String, byte[]> entry : webResources.entrySet()) {
                    try {
                        VirtualFile child = getDeploymentRoot().getChild(entry.getKey());
                        if (child.exists()) {
                            File file = child.getPhysicalFile();
                            try (FileOutputStream out = new FileOutputStream(file)) {
                                out.write(entry.getValue());
                            }
                        }
                    } catch (Exception e) {
                        ClassChangeMessages.ROOT_LOGGER.failedToupdateWebResource(entry.getKey(), e);
                    }
                }
                for (Map.Entry<String, byte[]> entry : updated.entrySet()) {
                    try {
                        VirtualFile child = getClassesRoot().getChild(entry.getKey());
                        File file = child.getPhysicalFile();
                        file.getParentFile().mkdirs();
                        try (FileOutputStream out = new FileOutputStream(file)) {
                            out.write(entry.getValue());
                        }
                    } catch (Exception e) {
                        ClassChangeMessages.ROOT_LOGGER.failedToupdateWebResource(entry.getKey(), e);
                    }
                }
            }
            List<ClassDefinition> modified = new ArrayList<>();
            List<AddedClass> added = new ArrayList<>();
            for (Map.Entry<String, byte[]> entry : updated.entrySet()) {
                String name = entry.getKey().substring(0, entry.getKey().length() - 6); //remove .class
                name = name.replace(File.separatorChar, '.');
                name = name.replace('/', '.');
                if (known.containsKey(entry.getKey())) {
                    try {
                        Class<?> existing = getClassLoader().loadClass(name);
                        modified.add(new ClassDefinition(existing, entry.getValue()));
                    } catch (ClassNotFoundException e) {
                        ClassChangeMessages.ROOT_LOGGER.failedToReplaceClassFile(name, e);
                    }
                } else {
                    added.add(new AddedClass(name, entry.getValue(), getClassLoader()));
                }
            }

            try {
                Fakereplace.redefine(modified.toArray(new ClassDefinition[modified.size()]), added.toArray(new AddedClass[added.size()]));
            } catch (Exception e) {
                doRedeployment();
            }
        } catch (IOException e) {
            ClassChangeMessages.ROOT_LOGGER.failedToScan(e);
        }
    }

    @Override
    public Path getAdditionalWebResourcesRoot() {
        return replacedResourcesDir.toPath();
    }


    @Override
    protected void handleNonInstigatedChange(List<ChangedClass> changedClasses) {
        for (ChangedClass c : changedClasses) {
            File file = new File(replacedClassesDir, c.getChangedClass().getName().replace('.', File.separatorChar) + ".class");
            file.getParentFile().mkdirs();
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(c.getData());
            } catch (IOException e) {
                ClassChangeMessages.ROOT_LOGGER.failedToWriteFile(file, e);
            }
        }
    }

    public void deploymentUnmounted() {
        try {
            File tempArchive = File.createTempFile("class-change", "temp-archive", new File(tempDir));
            Set<String> seen = new HashSet<>();
            boolean replacementRequired;
            try (FileOutputStream out = new FileOutputStream(tempArchive)) {
                try (ZipOutputStream zip = new ZipOutputStream(out)) {
                    replacementRequired = addContent(zip, "", seen);
                    if (replacementRequired) {
                        try (ZipFile existingZip = new ZipFile(deploymentContents.getPhysicalFile())) {
                            Enumeration<? extends ZipEntry> entries = existingZip.entries();
                            while (entries.hasMoreElements()) {
                                ZipEntry entry = entries.nextElement();
                                if (!seen.contains(entry.getName())) {
                                    zip.putNextEntry(new ZipEntry(entry.getName()));
                                    try (InputStream in = existingZip.getInputStream(entry)) {
                                        int r;
                                        byte[] buf = new byte[1024];
                                        while ((r = in.read(buf)) > 0) {
                                            zip.write(buf, 0, r);
                                        }
                                    }
                                    zip.closeEntry();
                                }
                            }
                        }
                    }
                }
            }
            if (replacementRequired) {
                deploymentContents.getPhysicalFile().delete();
                Files.move(tempArchive.toPath(), deploymentContents.getPhysicalFile().toPath());
            }

            tempArchive.delete();

        } catch (Exception e) {
            ClassChangeMessages.ROOT_LOGGER.failedToUpdateDeploymentArchive(getDeploymentUnit().getName(), e);
        }

        try {
            deleteRecursively(replacedResourcesDir.toPath());
        } catch (IOException e) {
            ClassChangeMessages.ROOT_LOGGER.failedToDeleteTempDir(e);
        }
    }

    private boolean addContent(ZipOutputStream zip, String currentPath, Set<String> seen) throws IOException {
        File file = new File(replacedResourcesDir, currentPath);
        boolean ret = false;
        for (File f : file.listFiles()) {
            String currentFile = currentPath.isEmpty() ? f.getName() : currentPath + "/" + f.getName();
            if (f.isDirectory()) {
                seen.add(currentFile + "/");
                zip.putNextEntry(new ZipEntry(currentFile + "/"));
                zip.closeEntry();
                if (addContent(zip, currentFile, seen)) {
                    ret = true;
                }
            } else {
                zip.putNextEntry(new ZipEntry(currentFile));
                try (FileInputStream in = new FileInputStream(f)) {
                    int r;
                    byte[] buf = new byte[1024];
                    while ((r = in.read(buf)) > 0) {
                        zip.write(buf, 0, r);
                    }
                }
                zip.closeEntry();
                seen.add(currentFile);
                ret = true;
            }
        }
        return ret;

    }
}
