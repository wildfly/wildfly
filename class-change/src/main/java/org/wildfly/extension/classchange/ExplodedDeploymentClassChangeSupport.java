/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.classchange;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.fakereplace.api.ChangedClass;
import org.fakereplace.api.ClassChangeAware;
import org.fakereplace.replacement.AddedClass;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.classchange.logging.ClassChangeMessages;

class ExplodedDeploymentClassChangeSupport extends AbstractClassChangeSupport implements DeploymentClassChangeSupport, ClassChangeAware {

    private final VirtualFile deploymentRoot;

    ExplodedDeploymentClassChangeSupport(DeploymentUnit deploymentUnit) {
        super(deploymentUnit);
        deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
    }

    @Override
    protected ExternalScan createScan() {
        return new ExternalScan() {
            @Override
            public void handleChangedExternalWebResourceFile(byte[] newVersion, String targetLocation) {
                try {
                    copyFile(newVersion, new File(deploymentRoot.getPhysicalFile(), targetLocation));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void handleChangedExternalClassFile(byte[] newVersion, String targetLocation) {
                try {
                    copyFile(newVersion, new File(getClassesRoot().getPhysicalFile(), targetLocation));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }

            @Override
            public void scanComplete(List<ClassDefinition> modified, List<AddedClass> addedClasses) throws IOException, ClassNotFoundException {
                List<VirtualFile> newFiles = new ArrayList<>();
                List<VirtualFile> modFiles = new ArrayList<>();
                //scan for all modified and new classes
                for (VirtualFile file : deploymentRoot.getChildrenRecursively()) {
                    if (file.getName().endsWith(CLASS)) {
                        Long modTime = getClassModificationTimes().get(file.getPathName());
                        if (modTime == null) {
                            newFiles.add(file);
                            getClassModificationTimes().put(file.getPathName(), file.getLastModified());
                        } else if (modTime != file.getLastModified()) {
                            modFiles.add(file);
                            getClassModificationTimes().put(file.getPathName(), file.getLastModified());
                        }
                    }
                }
                if (modFiles.isEmpty() && newFiles.isEmpty()) {
                    //if nothing has changed we just return
                    return;
                }
                for (VirtualFile i : modFiles) {
                    byte[] data = readFile(i);
                    //we infer the class name from the file name
                    //then try and load it to get the class definition
                    String className = i.getPathName().substring(getClassesRoot().getPathName().length() + 1, i.getPathName().length() - CLASS.length()).replace("/", ".");
                    Class<?> classDefinition = getClassLoader().loadClass(className);
                    modified.add(new ClassDefinition(classDefinition, data));
                }

                for (VirtualFile i : newFiles) {
                    byte[] data = readFile(i);
                    //we infer the class name from the file name
                    //then try and load it to get the class definition
                    String className = i.getPathName().substring(getClassesRoot().getPathName().length() + 1, i.getPathName().length() - CLASS.length()).replace("/", ".");
                    addedClasses.add(new AddedClass(className, data, getClassLoader()));
                }
            }
        };
    }

    @Override
    public void notifyChangedClasses(Map<String, byte[]> srcFiles, Map<String, byte[]> classFiles, Map<String, byte[]> webResources) {
        try {
            File classesRoot = getClassesRoot().getPhysicalFile();
            File deploymentRoot = this.deploymentRoot.getPhysicalFile();
            updateChangesClassesOnDisk(classesRoot, deploymentRoot, srcFiles, classFiles, webResources);
            scanForChangedClasses();
        } catch (IOException e) {
            ClassChangeMessages.ROOT_LOGGER.failedToScan(e);
        }
    }

    @Override
    public Path getAdditionalWebResourcesRoot() {
        return null;
    }

    @Override
    protected void handleNonInstigatedChange(List<ChangedClass> changedClasses) {
        try {
            File classesRoot = getClassesRoot().getPhysicalFile();
            for (ChangedClass c : changedClasses) {
                String path = c.getChangedClass().getName().replace(".", File.separator) + ".class";
                File targetFile = new File(classesRoot, path);
                try (FileOutputStream out = new FileOutputStream(targetFile)) {
                    out.write(c.getData());
                }
                getClassModificationTimes().put(path, targetFile.lastModified());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
