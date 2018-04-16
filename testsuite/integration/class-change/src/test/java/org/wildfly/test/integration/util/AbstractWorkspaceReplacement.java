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

package org.wildfly.test.integration.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public abstract class AbstractWorkspaceReplacement implements AutoCloseable {

    protected final File workspaceBase;
    protected final File workspaceClasses;
    protected final File workspaceJava;
    protected final File workspaceWebResources;
    private final Class<?> testClass;

    public AbstractWorkspaceReplacement(Class<?> testClass, List<String> webResources, List<Class<?>> classes) {
        try {
            this.testClass = testClass;
            workspaceBase = File.createTempFile("wf-class-change-tests", "workspace");
            workspaceBase.delete();
            workspaceBase.mkdir();
            workspaceClasses = new File(workspaceBase, "classes");
            workspaceClasses.mkdirs();
            workspaceJava = new File(workspaceBase, "src");
            workspaceJava.mkdirs();
            workspaceWebResources = new File(workspaceBase, "webapp");
            workspaceWebResources.mkdirs();

            //copy files to the workspace
            for (String resource : webResources) {
                File target = new File(workspaceWebResources, resource);
                try (InputStream res = testClass.getResourceAsStream(resource)) {
                    copy(target, res);
                }
                target.setLastModified(target.lastModified() - 3000);
            }
            for (Class<?> theClass : classes) {
                File target = new File(workspaceClasses, theClass.getName().replace('.', '/') + ".class");
                target.getParentFile().mkdirs();
                try (InputStream res = theClass.getResourceAsStream(theClass.getSimpleName() + ".class")) {
                    copy(target, res);
                }
                target.setLastModified(target.lastModified() - 3000);

                File javaFile = getJavaFileForClass(theClass);
                target = new File(workspaceJava, theClass.getName().replace('.', '/') + ".java");
                target.getParentFile().mkdirs();
                try (InputStream res = new FileInputStream(javaFile)) {
                    copy(target, res);
                }
                target.setLastModified(target.lastModified() - 3000);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getClassChangeProps() {
        Properties p = new Properties();
        p.put("web.resources.dir", workspaceWebResources.getAbsolutePath());
        p.put("classes.dir", workspaceClasses.getAbsolutePath());
        p.put("srcs.dir", workspaceJava.getAbsolutePath());
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        try {
            p.store(o, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return o.toByteArray();
    }

    public void replaceWebResource(String original, String newResource) {

        File target = new File(workspaceWebResources, original);
        try (InputStream in = testClass.getResourceAsStream(newResource)) {
            copy(target, in);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        deleteRecursively(workspaceBase);
    }

    public void addWebResource(String resource) {
        File target = new File(workspaceWebResources, resource);
        try (InputStream in = testClass.getResourceAsStream(resource)) {
            target.getParentFile().mkdirs();
            copy(target, in);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void queueClassReplacement(Class<?> original, Class<?> replacement);

    public abstract void queueAddClass(Class<?> theClass);

    public abstract void doReplacement();

    protected File getJavaFileForClass(Class<?> theClass) {
        String testsRoot = System.getProperty("jbossas.ts.integ.dir");
        StringBuilder sb = new StringBuilder(testsRoot);
        sb.append(File.separatorChar);
        sb.append("src");
        sb.append(File.separatorChar);
        sb.append("test");
        sb.append(File.separatorChar);
        sb.append("java");
        sb.append(File.separatorChar);
        sb.append(theClass.getName().replace('.', File.separatorChar));
        sb.append(".java");
        return new File(sb.toString());
    }

    public static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                deleteRecursively(f);
            }
        }
        file.delete();
    }

    public static void copy(File target, InputStream resourceAsStream) throws IOException {
        try (FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[2048];
            int r;
            while ((r = resourceAsStream.read(buffer)) > 0) {
                out.write(buffer, 0, r);
            }
        }
    }
}
