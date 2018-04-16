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

package org.wildfly.test.integration.changedetection;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.wildfly.test.integration.util.AbstractWorkspaceReplacement;

public abstract class JavaFileWorkspaceReplacementStrategy extends AbstractWorkspaceReplacement {

    public JavaFileWorkspaceReplacementStrategy(Class<?> testClass, List<String> webResources, List<Class<?>> classes) {
        super(testClass, webResources, classes);
    }

    @Override
    public void queueClassReplacement(Class<?> original, Class<?> replacement) {
        try {
            File javaFile = getJavaFileForClass(replacement);
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            int r;
            byte[] buff = new byte[2048];
            try (FileInputStream in = new FileInputStream(javaFile)) {
                while ((r = in.read(buff)) > 0) {
                    data.write(buff, 0, r);
                }
            }
            String contents = new String(data.toByteArray(), StandardCharsets.UTF_8);
            contents = contents.replace(replacement.getSimpleName(), original.getSimpleName()); //primitive, but should work for this simple case
            byte[] contentsBytes = contents.getBytes(StandardCharsets.UTF_8);

            doReplacement(original, contentsBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void doReplacement(Class<?> original, byte[] contentsBytes) throws IOException;

    @Override
    public void queueAddClass(Class<?> theClass) {
        try {
            File javaFile = getJavaFileForClass(theClass);
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            int r;
            byte[] buff = new byte[2048];
            try (FileInputStream in = new FileInputStream(javaFile)) {
                while ((r = in.read(buff)) > 0) {
                    data.write(buff, 0, r);
                }
            }
            byte[] bytes = data.toByteArray();
            doAdd(theClass, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void doAdd(Class<?> theClass, byte[] bytes) throws IOException;

}
