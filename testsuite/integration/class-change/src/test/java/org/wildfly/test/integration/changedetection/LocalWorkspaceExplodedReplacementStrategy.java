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
import java.util.function.Function;

import org.wildfly.test.integration.util.AbstractWorkspaceReplacement;

class LocalWorkspaceExplodedReplacementStrategy implements ExplodedReplacementStrategy {

    protected ExplodedDeploymentManager manager;

    private final Function<ExplodedDeploymentManager, AbstractWorkspaceReplacement> function;
    private AbstractWorkspaceReplacement replacement;

    LocalWorkspaceExplodedReplacementStrategy(Function<ExplodedDeploymentManager, AbstractWorkspaceReplacement> function) {
        this.function = function;
    }

    @Override
    public void init(ExplodedDeploymentManager manager) {
        try {
            this.manager = manager;
            replacement = function.apply(manager);
            //now create the class-change.properties
            File classChangeProps = new File(manager.getClassesRoot().getParent(), "class-change.properties");
            classChangeProps.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(classChangeProps)) {
                out.write(replacement.getClassChangeProps());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void replaceWebResource(String original, String newResource) {
        replacement.replaceWebResource(original, newResource);
    }

    @Override
    public void close() {
        replacement.close();
        replacement = null;
    }

    @Override
    public void addWebResource(String resource) {
        replacement.addWebResource(resource);
    }

    @Override
    public void replaceClass(Class<?> original, Class<?> replacement) {
        this.replacement.queueClassReplacement(original, replacement);
        this.replacement.doReplacement();
    }

    @Override
    public void addClass(Class<?> theClass) {
        this.replacement.queueAddClass(theClass);
        this.replacement.doReplacement();
    }
}
