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
import java.lang.instrument.ClassDefinition;
import java.util.List;

import org.fakereplace.replacement.AddedClass;
import org.wildfly.test.integration.util.AbstractClassReplacer;
import org.wildfly.test.integration.util.AbstractWorkspaceReplacement;

class ClassFileLocalWorkspaceReplacementStrategy extends AbstractWorkspaceReplacement {

    private final AbstractClassReplacer classReplacer = new AbstractClassReplacer() {
        @Override
        protected void doReplacement(ClassDefinition[] definitions, AddedClass[] newClasses) {
            try {
                for (ClassDefinition def : definitions) {
                    File target = new File(workspaceClasses, def.getDefinitionClass().getName().replace('.', File.separatorChar) + ".class");
                    try (FileOutputStream out = new FileOutputStream(target)) {
                        out.write(def.getDefinitionClassFile());
                    }
                }
                for (AddedClass def : newClasses) {
                    File target = new File(workspaceClasses, def.getClassName().replace('.', File.separatorChar) + ".class");
                    target.getParentFile().mkdirs();
                    try (FileOutputStream out = new FileOutputStream(target)) {
                        out.write(def.getData());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

    public ClassFileLocalWorkspaceReplacementStrategy(Class<?> testClass, List<String> webResources, List<Class<?>> classes) {
        super(testClass, webResources, classes);
    }

    @Override
    public void queueClassReplacement(Class<?> original, Class<?> replacement) {
        classReplacer.queueClassForReplacement(original, replacement);
    }

    @Override
    public void queueAddClass(Class<?> theClass) {
        classReplacer.addNewClass(theClass);
    }

    @Override
    public void doReplacement() {
        classReplacer.replaceQueuedClasses();
    }
}
