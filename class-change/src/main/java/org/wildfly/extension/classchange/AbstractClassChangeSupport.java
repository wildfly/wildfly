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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.ClassDefinition;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.fakereplace.api.ChangedClass;
import org.fakereplace.api.ClassChangeAware;
import org.fakereplace.api.NewClassData;
import org.fakereplace.core.Fakereplace;
import org.fakereplace.replacement.AddedClass;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.logging.ServerLogger;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Indexer;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.classchange.logging.ClassChangeMessages;

abstract class AbstractClassChangeSupport implements DeploymentClassChangeSupport, ClassChangeAware {

    protected static final String CLASS = ".class";
    protected static final String JAVA = ".java";
    private final DeploymentUnit deploymentUnit;
    private volatile ModuleClassLoader classLoader;
    private final List<ClassChangeListener> listeners = new CopyOnWriteArrayList<>();

    private final Map<String, Long> classModificationTimes = new ConcurrentHashMap<>();
    private final VirtualFile deploymentRoot;
    private volatile String externalClassFileLocation;
    private volatile String externalSrcFileLocation;
    private volatile String externalWebResourceLocation;
    protected volatile boolean instigatingChange;

    AbstractClassChangeSupport(DeploymentUnit deploymentUnit) {
        this.deploymentUnit = deploymentUnit;
        deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
    }

    public void setClassLoader(ModuleClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void doInitialScan() {
        classModificationTimes.putAll(getClassModificationTimes());
    }


    @Override
    public void scanForChangedClasses() {
        ExternalScan scan = createScan();
        try {
            VirtualFile classesRoot = getClassesRoot();
            Map<String, Long> knownClasses = getKnownClasses();
            Map<String, Long> knownWebResources = getKnownWebResources();

            Map<String, Long> changedSourceFiles = new HashMap<>();
            if (getExternalSrcFileLocation() != null) {
                File extRoot = new File(getExternalSrcFileLocation());
                if (extRoot.exists()) {
                    doExternalSrcScan(extRoot, "", knownClasses, changedSourceFiles);
                }
            }
            if (getExternalClassFileLocation() != null) {
                //now we know what we are comparing
                //we scan the external directory and copy anything that is newer
                File extRoot = new File(getExternalClassFileLocation());
                if (extRoot.exists()) {
                    doExternalClassScan(extRoot, "", knownClasses, changedSourceFiles, scan);
                }
            }
            if (getExternalWebResourceLocation() != null) {

                File extRoot = new File(getExternalWebResourceLocation());
                if (extRoot.exists()) {
                    doExternalWebResourceScan(extRoot, "", knownWebResources, scan);
                }
            }
            if (!changedSourceFiles.isEmpty()) {
                ClassChangeMessages.ROOT_LOGGER.compilingChangedClassFiles(changedSourceFiles.keySet());
                ClassLoaderCompiler compiler = new ClassLoaderCompiler(classLoader, Paths.get(getExternalSrcFileLocation()), new ArrayList<>(changedSourceFiles.keySet()));
                compiler.compile();
                for (Map.Entry<String, ByteArrayOutputStream> entry : compiler.getOutput().entrySet()) {
                    scan.handleChangedExternalClassFile(entry.getValue().toByteArray(), entry.getKey() + CLASS);
                }
            }
            List<AddedClass> added = new ArrayList<>();
            List<ClassDefinition> modifiedClasses = new ArrayList<>();

            scan.scanComplete(modifiedClasses, added);
            if (modifiedClasses.isEmpty() && added.isEmpty()) {
                return;
            }
            List<String> modNames = new ArrayList<>();
            List<String> addNames = new ArrayList<>();
            for (ClassDefinition m : modifiedClasses) {
                modNames.add(m.getDefinitionClass().getName());
            }
            for (AddedClass a : added) {
                addNames.add(a.getClassName());
            }
            ClassChangeMessages.ROOT_LOGGER.attemptingToReplaceClasses(modNames, addNames);
            instigatingChange = true;
            try {
                Fakereplace.redefine(modifiedClasses.toArray(new ClassDefinition[modifiedClasses.size()]), added.toArray(new AddedClass[added.size()]));
            } catch (Exception e) {
                doRedeployment();
            } finally {
                instigatingChange = false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void doRedeployment() {
        ClassChangeMessages.ROOT_LOGGER.attemptingRedeployment();
        long start = System.currentTimeMillis();
        ServiceController<?> controller = deploymentUnit.getServiceRegistry().getRequiredService(deploymentUnit.getServiceName());
        controller.addListener(new LifecycleListener() {
            @Override
            public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                if (event == LifecycleEvent.DOWN) {
                    controller.setMode(ServiceController.Mode.ACTIVE);
                }
            }
        });
        controller.setMode(ServiceController.Mode.NEVER);
        try {
            controller.getServiceContainer().awaitStability();
            long done = System.currentTimeMillis();
            ClassChangeMessages.ROOT_LOGGER.redeploymentComplete(done - start);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private void doExternalWebResourceScan(File externalClassesRoot, String currentPath, Map<String, Long> knownWebResources, ExternalScan scan) {
        if (currentPath.equals("WEB-INF")) {
            return;
        }
        File current = new File(externalClassesRoot, currentPath);
        for (String part : current.list()) {
            String fileName = currentPath + (currentPath.isEmpty() ? "" : File.separatorChar) + part;
            File f = new File(current, part);
            if (f.isDirectory()) {
                doExternalWebResourceScan(externalClassesRoot, fileName, knownWebResources, scan);
            } else {
                Long serverTime = knownWebResources.get(fileName.replace(File.separatorChar, '/'));
                if (serverTime == null || serverTime < f.lastModified()) {
                    scan.handleChangedExternalWebResourceFile(readFile(f), fileName);
                }
            }
        }
    }

    protected abstract ExternalScan createScan();

    /**
     * Handles changes that were not instigated by the subsystem, in practice this likely means changes sent by a debugger
     *
     * @param changedClasses The changed classes
     */
    protected abstract void handleNonInstigatedChange(List<ChangedClass> changedClasses);


    private void doExternalSrcScan(File externalSrcsRoot, String currentPath, Map<String, Long> knownClassFiles, Map<String, Long> changedSourceFiles) {
        File current = new File(externalSrcsRoot, currentPath);
        for (String part : current.list()) {
            File f = new File(current, part);
            String fileName = currentPath + (currentPath.isEmpty() ? "" : File.separatorChar) + part;
            if (f.isDirectory()) {
                doExternalSrcScan(externalSrcsRoot, fileName, knownClassFiles, changedSourceFiles);
            } else if (part.endsWith(JAVA)) {
                String noExtension = fileName.substring(0, fileName.length() - 5);
                String replaced = noExtension.replace(File.separatorChar, '/');
                Long serverTime = knownClassFiles.get(replaced + CLASS);
                if (serverTime == null || serverTime < f.lastModified()) {
                    //put the source file into the map
                    changedSourceFiles.put(replaced, f.lastModified());
                }
            }
        }
    }

    private void doExternalClassScan(File externalClassesRoot, String currentPath, Map<String, Long> knownClasses, Map<String, Long> changedSourceFiles, ExternalScan scan) {
        File current = new File(externalClassesRoot, currentPath);
        for (String part : current.list()) {
            File f = new File(current, part);
            String fileName = currentPath + (currentPath.isEmpty() ? "" : File.separatorChar) + part;
            if (f.isDirectory()) {
                doExternalClassScan(externalClassesRoot, fileName, knownClasses, changedSourceFiles, scan);
            } else if (part.endsWith(CLASS)) {
                Long serverTime = knownClasses.get(fileName);
                if (serverTime == null || serverTime < f.lastModified()) {
                    String noExtension = part.substring(0, part.length() - 5).replace(File.separatorChar, '/');
                    Long srcChanged = changedSourceFiles.get(noExtension);
                    if (srcChanged == null || srcChanged < f.lastModified()) {
                        //we update the class if it is newer than the src file
                        //this means that it a manual compile has been run and we should use the
                        //output of that, rather than compiling ourselves
                        changedSourceFiles.remove(noExtension);
                        scan.handleChangedExternalClassFile(readFile(f), fileName);
                    }
                }
            }
        }
    }


    @Override
    public void addListener(ClassChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public Map<String, Long> getKnownClasses() {
        VirtualFile classesRoot = getClassesRoot();
        Map<String, Long> ret = new HashMap<>();
        findClasses("", classesRoot, ret);
        return ret;
    }

    private void findClasses(String currentPath, VirtualFile serverClassesRoot, Map<String, Long> found) {
        VirtualFile serverCurrent = serverClassesRoot.getChild(currentPath);
        for (VirtualFile vf : serverCurrent.getChildren()) {
            String part = vf.getName();
            String fullPath = currentPath + (currentPath.isEmpty() ? "" : File.separatorChar) + part;
            if (vf.isDirectory()) {
                findClasses(fullPath, serverClassesRoot, found);
            } else if (part.endsWith(CLASS)) {
                found.put(fullPath.replace(File.separatorChar, '/'), vf.getLastModified());
            }
        }
    }

    @Override
    public Map<String, Long> getKnownWebResources() {
        Map<String, Long> ret = new HashMap<>();
        findWebResources("", deploymentRoot, ret);
        return ret;
    }

    private void findWebResources(String currentPath, VirtualFile serverRoot, Map<String, Long> found) {
        if (currentPath.equals("/WEB-INF")) {
            return;
        }
        VirtualFile serverCurrent = serverRoot.getChild(currentPath);
        for (VirtualFile vf : serverCurrent.getChildren()) {
            String part = vf.getName();
            String fullPart = currentPath + File.separatorChar + part;
            if (vf.isDirectory()) {
                findWebResources(fullPart, serverRoot, found);
            } else {
                found.put(fullPart.replace(File.separatorChar, '/'), vf.getLastModified());
            }
        }
    }

    void setExternalClassFileLocation(String location) {
        externalClassFileLocation = location;
    }

    protected VirtualFile getClassesRoot() {
        //if we need to scan the external location and figure out what has changed
        //first we need to figure out the deployment classes root
        //this will be either the root of deployment or WEB-INF/classes
        VirtualFile classesRoot = deploymentRoot;
        if (deploymentUnit.getName().endsWith(".war")) {
            classesRoot = classesRoot.getChild("WEB-INF/classes"); //TODO: this could be better
        }
        return classesRoot;
    }

    protected static void deleteRecursively(Path file) throws IOException {
        if (Files.exists(file)) {
            Files.walkFileTree(file, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

            });
        }
    }

    protected static void copyFile(File src, File dst) throws IOException {
        dst.getParentFile().mkdirs();
        try (InputStream in = new BufferedInputStream(new FileInputStream(src))) {
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(dst))) {
                byte[] buf = new byte[1024];
                int r;
                while ((r = in.read(buf)) > 0) {
                    out.write(buf, 0, r);
                }
            }
        }
    }

    protected static void copyFile(byte[] src, File dst) throws IOException {
        dst.getParentFile().mkdirs();
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(dst))) {
            out.write(src);
        }
    }

    protected static byte[] readFile(VirtualFile i) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = i.openStream()) {
            byte[] buf = new byte[1024];
            int r;
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
        }
        return out.toByteArray();
    }

    protected static byte[] readFile(File i) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = new FileInputStream(i)) {
                byte[] buf = new byte[1024];
                int r;
                while ((r = in.read(buf)) > 0) {
                    out.write(buf, 0, r);
                }
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterChange(List<ChangedClass> changedClasses, List<NewClassData> newClasses) {
        List<ClassChangeListener.ChangedClasssDefinition> filteredChanged = new ArrayList<>();
        List<ClassChangeListener.NewClassDefinition> filteredAdded = new ArrayList<>();
        Indexer i = new Indexer();
        for (ChangedClass changed : changedClasses) {
            if (classLoader.equals(changed.getChangedClass().getClassLoader())) {
                try {
                    filteredChanged.add(new ClassChangeListener.ChangedClasssDefinition(changed.getChangedClass(), i.index(new ByteArrayInputStream(changed.getData()))));
                } catch (IOException e) {
                    //should not happen
                    ServerLogger.AS_ROOT_LOGGER.cannotIndexClass(changed.getChangedClass().getName(), changed.getChangedClass().getClassLoader().toString(), e);
                }
            }
        }
        for (NewClassData added : newClasses) {
            if (classLoader.equals(added.getClassLoader())) {
                try {
                    ClassInfo classInfo = i.index(new DataInputStream(new ByteArrayInputStream(added.getData())));
                    filteredAdded.add(new ClassChangeListener.NewClassDefinition(added.getClassName(), added.getClassLoader(), added.getData(), classInfo));
                } catch (IOException e) {
                    //should not happen
                    ClassChangeMessages.ROOT_LOGGER.cannotIndexClass(added.getClassName(), added.getClassLoader().toString(), e);
                }
            }
        }
        if (filteredAdded.isEmpty() && filteredChanged.isEmpty()) {
            return;
        }
        if (!instigatingChange) {
            //TODO: we need to update the local file system with these changes
            handleNonInstigatedChange(changedClasses);

        }

        for (ClassChangeListener listener : listeners) {
            listener.classesReplaced(Collections.unmodifiableList(filteredChanged), Collections.unmodifiableList(filteredAdded));
        }
    }


    /**
     * utility method to handle common tasks related to {@link #notifyChangedClasses(Map, Map, Map)}
     *
     * @param srcFiles
     * @param classFiles
     * @param webResources
     * @return A map of changed/updated class file names to their contents
     */
    public Map<String, byte[]> updateChangesClassesOnDisk(File classesDir, File resourcesDir, Map<String, byte[]> srcFiles, Map<String, byte[]> classFiles, Map<String, byte[]> webResources) throws IOException {
        Map<String, byte[]> ret = new HashMap<>();
        //copy the classes
        for (Map.Entry<String, byte[]> entry : classFiles.entrySet()) {
            ret.put(entry.getKey(), entry.getValue());
            File file = new File(classesDir, entry.getKey());
            file.getParentFile().mkdirs();
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(entry.getValue());
            }
        }
        //copy the web resources
        for (Map.Entry<String, byte[]> entry : webResources.entrySet()) {
            File file = new File(resourcesDir, entry.getKey());
            file.getParentFile().mkdirs();
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(entry.getValue());
            }
        }
        if (!srcFiles.isEmpty()) {
            //now we need to compile the src files
            Path tempDir = Files.createTempDirectory("org.fakereplace.src");
            try {
                File root = tempDir.toFile();
                for (Map.Entry<String, byte[]> entry : srcFiles.entrySet()) {
                    File file = new File(root, entry.getKey());
                    file.getParentFile().mkdirs();
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        outputStream.write(entry.getValue());
                    }
                }
                List<String> baseNames = new ArrayList<>();
                for (String name : srcFiles.keySet()) {
                    baseNames.add(name.substring(0, name.lastIndexOf('.')));
                }
                ClassChangeMessages.ROOT_LOGGER.compilingChangedClassFiles(baseNames);
                ClassLoaderCompiler compiler = new ClassLoaderCompiler(classLoader, tempDir, baseNames);
                compiler.compile();
                for (Map.Entry<String, ByteArrayOutputStream> entry : compiler.getOutput().entrySet()) {
                    String fn = entry.getKey() + ".class";
                    File f = new File(classesDir, fn);
                    f.getParentFile().mkdirs();
                    byte[] classBytes = entry.getValue().toByteArray();
                    ret.put(fn, classBytes);
                    try (FileOutputStream out = new FileOutputStream(f)) {
                        out.write(classBytes);
                    }
                }
            } finally {
                deleteRecursively(tempDir);
            }
        }
        return ret;
    }

    public void setExternalSourceFileLocation(String property) {
        externalSrcFileLocation = property;
    }

    public void setExternalWebResourceLocation(String externalWebResourceLocation) {
        this.externalWebResourceLocation = externalWebResourceLocation;
    }

    public DeploymentUnit getDeploymentUnit() {
        return deploymentUnit;
    }

    public List<ClassChangeListener> getListeners() {
        return listeners;
    }

    public ModuleClassLoader getClassLoader() {
        return classLoader;
    }

    public Map<String, Long> getClassModificationTimes() {
        return classModificationTimes;
    }

    public VirtualFile getDeploymentRoot() {
        return deploymentRoot;
    }

    public String getExternalClassFileLocation() {
        return externalClassFileLocation;
    }

    public String getExternalSrcFileLocation() {
        return externalSrcFileLocation;
    }

    public String getExternalWebResourceLocation() {
        return externalWebResourceLocation;
    }

    public boolean isInstigatingChange() {
        return instigatingChange;
    }


    interface ExternalScan {

        void handleChangedExternalWebResourceFile(byte[] newVersion, String targetLocation);

        void handleChangedExternalClassFile(byte[] newVersion, String targetLocation);

        /**
         * Called when the external part of the scan is complete. This callback allows for additional changed to be queued
         * <p>
         * It will generally be used by exploded archives to add any non-external changes to the changed class list
         *
         * @param modified     The mutable modified class list
         * @param addedClasses The mutable added class this
         * @throws IOException
         * @throws ClassNotFoundException
         */
        void scanComplete(List<ClassDefinition> modified, List<AddedClass> addedClasses) throws IOException, ClassNotFoundException;
    }
}
