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
package org.wildfly.test.integration.security.picketlink.util;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.xnio.IoUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Utility class with some convenience methods to create and remove modules.</p>
 *
 * @author Pedro Igor
 */
public class TestModule {

    private final String moduleName;
    private final File moduleXml;
    private final List<JavaArchive> resources = new ArrayList<>();

    /**
     * <p>Creates a new module with the given name and module definition.</p>
     *
     * @param moduleName The name of the module.
     * @param moduleXml The module definition file..
     */
    public TestModule(String moduleName, File moduleXml) {
        if (!moduleXml.exists()) {
            throw new IllegalArgumentException("The module definition must exists.");
        }

        this.moduleName = moduleName;
        this.moduleXml = moduleXml;
    }

    /**
     * <p>Creates the module directory. If the module already exists, it will deleted first.</p>
     *
     * @throws IOException If any error occurs during the module creation.
     */
    public void create() throws IOException {
        create(true);
    }

    /**
     * <p>Creates the module directory.</p>
     *
     * @param deleteFirst If the module already exists, this argument specifies if it should be deleted before continuing.
     *
     * @throws IOException
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
            copyFile(new File(mainDirectory, "module.xml"), new FileInputStream(this.moduleXml));
        } catch (IOException e) {
            throw new RuntimeException("Could not create module definition.", e);
        }

        for (JavaArchive resourceRoot : this.resources) {
            FileOutputStream jarFile = null;

            try {
                Indexer indexer = new Indexer();

                for (Node content : resourceRoot.getContent().values()) {
                    if (content.getPath().get().endsWith(".class")) {
                        indexer.index(content.getAsset().openStream());
                    }
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

        this.resources.add(resource);

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
                throw new RuntimeException("Could not delete [" + file.getPath() + ".");
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
            IoUtils.safeClose(out);
        }
    }

    public String getName() {
        return moduleName;
    }
}
