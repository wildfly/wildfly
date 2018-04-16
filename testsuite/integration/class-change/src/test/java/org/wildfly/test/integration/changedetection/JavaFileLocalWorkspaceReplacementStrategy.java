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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class JavaFileLocalWorkspaceReplacementStrategy extends JavaFileWorkspaceReplacementStrategy {

    public JavaFileLocalWorkspaceReplacementStrategy(Class<?> testClass, List<String> webResources, List<Class<?>> classes) {
        super(testClass, webResources, classes);
    }

    @Override
    public void doReplacement() {

    }

    @Override
    protected void doReplacement(Class<?> original, byte[] contentsBytes) throws IOException {
        File target = new File(workspaceJava, original.getName().replace('.', File.separatorChar) + ".java");
        try (FileOutputStream out = new FileOutputStream(target)) {
            out.write(contentsBytes);
        }
    }

    @Override
    protected void doAdd(Class<?> theClass, byte[] bytes) throws IOException {
        File target = new File(workspaceJava, theClass.getName().replace('.', File.separatorChar) + ".java");
        target.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(target)) {
            out.write(bytes);
        }
    }

}
