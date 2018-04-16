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

import java.lang.instrument.ClassDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.fakereplace.replacement.AddedClass;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

public abstract class AbstractClassReplacer {

    private final Map<String, String> nameReplacements = new HashMap<>();

    private final Map<Class<?>, Class<?>> queuedClassReplacements = new LinkedHashMap<>();

    private final List<Class<?>> addedClasses = new ArrayList<>();

    private final ClassPool pool = new ClassPool();

    public AbstractClassReplacer() {
        pool.appendSystemPath();
        pool.appendClassPath(new LoaderClassPath(getClass().getClassLoader()));
    }

    public void queueClassForReplacement(Class<?> oldClass, Class<?> newClass) {
        queuedClassReplacements.put(oldClass, newClass);
    }

    public void addNewClass(Class<?> definition) {
        addedClasses.add(definition);
    }

    public void replaceQueuedClasses() {
        try {
            ClassDefinition[] definitions = new ClassDefinition[queuedClassReplacements.size()];
            AddedClass[] newClasses = new AddedClass[addedClasses.size()];
            for (Class<?> o : queuedClassReplacements.keySet()) {
                Class<?> n = queuedClassReplacements.get(o);
                String newName = o.getName();
                String oldName = n.getName();
                nameReplacements.put(oldName, newName);
            }

            int count = 0;
            for (Class<?> o : queuedClassReplacements.keySet()) {
                Class<?> n = queuedClassReplacements.get(o);
                CtClass nc = pool.get(n.getName());

                if (nc.isFrozen()) {
                    nc.defrost();
                }

                for (String oldName : nameReplacements.keySet()) {
                    String newName = nameReplacements.get(oldName);
                    nc.replaceClassName(oldName, newName);
                }
                nc.setName(o.getName());
                ClassDefinition cd = new ClassDefinition(o, nc.toBytecode());
                definitions[count++] = cd;
            }
            count = 0;
            for (Class<?> o : addedClasses) {
                CtClass nc = pool.get(o.getName());

                if (nc.isFrozen()) {
                    nc.defrost();
                }

                for (String newName : nameReplacements.keySet()) {
                    String oldName = nameReplacements.get(newName);
                    nc.replaceClassName(newName, oldName);
                }
                AddedClass ncd = new AddedClass(o.getName(), nc.toBytecode(), o.getClassLoader());
                newClasses[count++] = ncd;
            }

            doReplacement(definitions, newClasses);

            queuedClassReplacements.clear();
            addedClasses.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    protected abstract void doReplacement(ClassDefinition[] definitions, AddedClass[] newClasses);
}
