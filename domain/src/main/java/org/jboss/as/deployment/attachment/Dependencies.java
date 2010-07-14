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

package org.jboss.as.deployment.attachment;

import org.jboss.as.deployment.AttachmentKey;
<<<<<<< HEAD:domain/src/main/java/org/jboss/as/deployment/attachment/Dependencies.java
import org.jboss.as.deployment.item.ModuleDeploymentItem;
=======
import org.jboss.as.deployment.descriptor.ModuleConfig;
>>>>>>> bf5e5784e281e51c89497aee3feab2adcaf0b0ea:domain/src/main/java/org/jboss/as/deployment/attachment/Dependencies.java
import org.jboss.as.deployment.unit.DeploymentUnitContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Mutable collection of module dependencies.
 *
 * @author John E. Bailey
 */
public class Dependencies {
    public static final AttachmentKey<Dependencies> KEY = AttachmentKey.create(Dependencies.class);

    private final Set<ModuleConfig.Dependency> dependencies = new HashSet<ModuleConfig.Dependency>();

    /**
     * Add a dependency to a DeploymentUnitContext.  Adding the attachment if needed.
     *
     * @param context    The DeploymentUnitContext
     * @param dependency The dependency to add
     */
    public static void addDependency(final DeploymentUnitContext context, final ModuleConfig.Dependency dependency) {
        Dependencies dependencies = context.getAttachment(KEY);
        if(dependencies == null) {
            dependencies = new Dependencies();
            context.putAttachment(KEY, dependencies);
        }
        dependencies.dependencies.add(dependency);
    }

    public static Dependencies getAttachedDependencies(DeploymentUnitContext context) {
        return context.getAttachment(KEY);
    }

    /**
     * Get the dependencies for a this attachment.
     *
     * @return The dependencies
     */
    public ModuleConfig.Dependency[] getDependencies() {
        return new ArrayList<ModuleConfig.Dependency>(dependencies).toArray(new ModuleConfig.Dependency[dependencies.size()]);
    }
}
