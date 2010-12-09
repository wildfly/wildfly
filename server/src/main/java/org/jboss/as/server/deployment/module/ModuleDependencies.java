/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.module;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Mutable collection of module dependencies.
 *
 * @author John E. Bailey
 */
public class ModuleDependencies {
    public static final AttachmentKey<ModuleDependencies> KEY = AttachmentKey.create(ModuleDependencies.class);

    private final Set<ModuleDependency> dependencies = new HashSet<ModuleDependency>();

    /**
     * Add a dependency to a DeploymentUnitContext.  Adding the attachment if needed.
     *
     * @param context    The DeploymentUnitContext
     * @param dependency The dependency to add
     */
    public static void addDependency(final DeploymentUnit context, final ModuleDependency dependency) {
        ModuleDependencies dependencies = context.getAttachment(KEY);
        if(dependencies == null) {
            dependencies = new ModuleDependencies();
            context.putAttachment(KEY, dependencies);
        }
        dependencies.dependencies.add(dependency);
    }

    public static ModuleDependencies getAttachedDependencies(DeploymentUnit context) {
        return context.getAttachment(KEY);
    }

    /**
     * Get the dependencies for a this attachment.
     *
     * @return The dependencies
     */
    public ModuleDependency[] getDependencies() {
        return new ArrayList<ModuleDependency>(dependencies).toArray(new ModuleDependency[dependencies.size()]);
    }
}
