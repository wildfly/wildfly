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

package org.jboss.as.patching.loader;

import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The patching boot module loader.
 *
 * @author Emanuel Muckenhuber
 */
public final class BootLoader extends ModuleLoader {

    private final File[] repoRoots;
    private final LocalModuleLoader localModuleLoader;
    public BootLoader() {
        repoRoots = createModulePath();
        localModuleLoader = new LocalModuleLoader(repoRoots);
        System.out.println(this);
    }

    @Override
    protected ModuleSpec findModule(final ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        try {
            final Method method = LocalModuleLoader.class.getDeclaredMethod("findModule", ModuleIdentifier.class);
            method.setAccessible(true);
            return (ModuleSpec) method.invoke(localModuleLoader, moduleIdentifier);
        } catch (NoSuchMethodException e) {
            throw new ModuleLoadException(e);
        } catch (InvocationTargetException e) {
            throw new ModuleLoadException(e);
        } catch (IllegalAccessException e) {
            throw new ModuleLoadException(e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("patch module loader @").append(Integer.toHexString(hashCode())).append(" (roots: ");
        final int repoRootsLength = repoRoots.length;
        for (int i = 0; i < repoRootsLength; i++) {
            final File root = repoRoots[i];
            b.append(root);
            if (i != repoRootsLength - 1) {
                b.append(',');
            }
        }
        b.append(')');
        return b.toString();
    }

    private static File[] createModulePath() {
        final String jbossHome = System.getProperty("jboss.home", System.getenv("JBOSS_HOME"));
        final String modulePath = System.getProperty("module.path", System.getenv("JAVA_MODULEPATH"));
        final File root = jbossHome == null ? new File(".") : new File(jbossHome); // TODO
        final File[] files = modulePath == null ? new File[0] : getFiles(modulePath, 0, 0);
        try {
            return PatchModulePathFactory.load(root, files);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File[] getFiles(final String modulePath, final int stringIdx, final int arrayIdx) {
        final int i = modulePath.indexOf(File.pathSeparatorChar, stringIdx);
        final File[] files;
        if (i == -1) {
            files = new File[arrayIdx + 1];
            files[arrayIdx] = new File(modulePath.substring(stringIdx)).getAbsoluteFile();
        } else {
            files = getFiles(modulePath, i + 1, arrayIdx + 1);
            files[arrayIdx] = new File(modulePath.substring(stringIdx, i)).getAbsoluteFile();
        }
        return files;
    }

}
