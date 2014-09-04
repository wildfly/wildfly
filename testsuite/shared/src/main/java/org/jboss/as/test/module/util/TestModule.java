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
package org.jboss.as.test.module.util;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * <p>Utility class with some convenience methods to create and remove modules.</p>
 *
 * @author Pedro Igor
 */
public class TestModule {

    private static final Logger log  = Logger.getLogger(TestModule.class);

    private final String moduleName;
    private final File moduleXml;
    private final String[] dependencies;
    private final List<ClassCallback> classCallbacks = new ArrayList<TestModule.ClassCallback>();
    private final List<JavaArchive> resources = new ArrayList<JavaArchive>();


    /**
     * <p>Creates a new module with the given name and module definition.</p>
     *
     * @param moduleName The name of the module.
     * @param moduleXml The module definition file.
     */
    public TestModule(String moduleName, File moduleXml) {
        if (!moduleXml.exists()) {
            throw new IllegalArgumentException("The module definition must exists.");
        }

        this.moduleName = moduleName;
        this.moduleXml = moduleXml;
        this.dependencies = null;
    }

    /**
     * <p>Creates a new module with the given name and module dependencies. The module.xml will be generated</p>
     *
     * @param moduleName The name of the module.
     * @param moduleXml The module definition file.
     */
    public TestModule(String moduleName, String...dependencies) {
        this.moduleName = moduleName;
        this.moduleXml = null;
        this.dependencies = dependencies;
    }

    /**
     * <p>Creates the module directory. If the module already exists, it will deleted first.</p>
     *
     * @throws java.io.IOException If any error occurs during the module creation.
     */
    public void create() throws IOException {
        create(true);
    }


    /**
     * Add a callback to handle classes being added
     *
     * @param callback the call back to add
     * @return this
     */
    public TestModule addClassCallback(ClassCallback callback) {
        classCallbacks.add(callback);
        return this;
    }

    /**
     * <p>Creates the module directory.</p>
     *
     * @param deleteFirst If the module already exists, this argument specifies if it should be deleted before continuing.
     *
     * @throws java.io.IOException
     */
    public void create(boolean deleteFirst) throws IOException {
        File moduleDirectory = getModuleDirectory();

        if (moduleDirectory.exists()) {
            if (!deleteFirst) {
                throw new IllegalArgumentException(moduleDirectory + " already exists.");
            }

            remove();
        }

        File mainDirectory = new File(moduleDirectory, "main");

        if (!mainDirectory.mkdirs()) {
            throw new IllegalArgumentException("Could not create " + mainDirectory);
        }

        try {
            if (moduleXml != null) {
                copyFile(new File(mainDirectory, "module.xml"), new FileInputStream(this.moduleXml));
            } else {
                generateModuleXml(mainDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create module definition.", e);
        }

        for (JavaArchive resourceRoot : this.resources) {

            FileOutputStream jarFile = null;

            try {
                Indexer indexer = new Indexer();

                List<Class<?>> classes = new ArrayList<Class<?>>();
                for (Node content : resourceRoot.getContent().values()) {
                    final String path = content.getPath().get();
                    if (path.endsWith(".class")) {
                        indexer.index(content.getAsset().openStream());
                        if (classCallbacks.size() > 0) {
                            //TODO load class
                            String usePath = path.startsWith("/") ? path.substring(1, path.length() - 6) : path.substring(0, path.length() - 6);
                            usePath = usePath.replaceAll("/", ".");
                            Class<?> clazz = Class.forName(usePath);
                            classes.add(clazz);
                        }
                    }
                }
                for (ClassCallback callback : classCallbacks) {
                    callback.classesAdded(resourceRoot, classes);
                }
                Index index = indexer.complete();
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                IndexWriter writer = new IndexWriter(data);

                writer.write(index);

                resourceRoot.addAsManifestResource(new ByteArrayAsset(data.toByteArray()), "jandex.idx");

                jarFile = new FileOutputStream(new File(mainDirectory, resourceRoot.getName()));
                resourceRoot.as(ZipExporter.class).exportTo(jarFile);
            } catch (Exception e) {
                throw new RuntimeException("Could not create module resource [" + resourceRoot.getName() + ".", e);
            } finally {
                if (jarFile != null) {
                    jarFile.flush();
                    jarFile.close();
                }
            }
        }
    }

    /**
     * <p>Removes the module from the modules directory. This operation can not be reverted.</p>
     */
    public void remove() {
        remove(getModuleDirectory());
    }

    /**
     * <p>Creates a {@link org.jboss.shrinkwrap.api.spec.JavaArchive} instance that will be used to create a jar file inside the
     * module's directory.</p>
     *
     * <p>The name of the archive must match one of the <code>resource-root</code> definitions defined in the module
     * definition.</p>
     *
     * @param fileName The name of the archive.
     *
     * @return
     */
    public JavaArchive addResource(String fileName) {
        JavaArchive resource = ShrinkWrap.create(JavaArchive.class, fileName);
        if (resources.size() == 0) {
            //Add the test module to the first jar in the module to avoid having to do that from the tests
            resource.addClass(TestModule.class);
        }
        resources.add(resource);
        return resource;
    }

    private void remove(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (String name : file.list()) {
                    remove(new File(file, name));
                }
            }

            if (!file.delete()) {
                //This will often not work on Windows
                log.error("Could not delete [" + file.getPath() + ".");
            }
        } else {
            throw new IllegalStateException("Module [" + this.moduleName + "] does not exists.");
        }
    }

    private File getModuleDirectory() {
        return new File(getModulesDirectory(), this.moduleName.replace('.', File.separatorChar));
    }

    private File getModulesDirectory() {
        String modulePath = System.getProperty("module.path", null);

        if (modulePath == null) {
            String jbossHome = System.getProperty("jboss.home", null);

            if (jbossHome == null) {
                throw new IllegalStateException("Neither -Dmodule.path nor -Djboss.home were set");
            }

            modulePath = jbossHome + File.separatorChar + "modules";
        } else {
            modulePath = modulePath.split(File.pathSeparator)[0];
        }

        File moduleDir = new File(modulePath);

        if (!moduleDir.exists()) {
            throw new IllegalStateException("Determined module path does not exist");
        }

        if (!moduleDir.isDirectory()) {
            throw new IllegalStateException("Determined module path is not a dir");
        }

        return moduleDir;
    }

    private static void copyFile(File target, InputStream src) throws IOException {
        final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target));
        try {
            int i = src.read();
            while (i != -1) {
                out.write(i);
                i = src.read();
            }
        } finally {
            out.close();
        }
    }

    public String getName() {
        return moduleName;
    }

    private void generateModuleXml(File mainDirectory) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(mainDirectory, "module.xml")));
        try {
            writer.write("<module xmlns=\"urn:jboss:module:1.1\" name=\"" + moduleName + "\">");
            writer.write("<resources>");
            for (JavaArchive jar : resources) {
                writer.write("<resource-root path=\"" + jar.getName() + "\"/>");
            }
            writer.write("</resources>");
            writer.write("<dependencies>");
            for (String dependency : dependencies) {
                writer.write("<module name=\"" + dependency + "\"/>");
            }
            writer.write("</dependencies>");
            writer.write("</module>");
        } finally {
            writer.close();
        }
    }

    public interface ClassCallback {
        void classesAdded(JavaArchive jar, final List<Class<?>> classes);
    }
}
