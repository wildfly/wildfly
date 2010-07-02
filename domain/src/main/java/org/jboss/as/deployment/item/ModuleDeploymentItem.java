/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.deployment.item;

import java.io.Serializable;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.BatchBuilder;

/**
 * A deployment item which defines a module.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleDeploymentItem implements DeploymentItem {

    private static final long serialVersionUID = 210753378958448029L;

    private final ModuleIdentifier identifier;
    private final Dependency[] dependencies;
    private final Resource[] resources;

    public ModuleDeploymentItem(final ModuleIdentifier identifier, final Dependency[] dependencies, final Resource[] resources) {
        this.identifier = identifier;
        this.dependencies = dependencies;
        this.resources = resources;
    }

    public void install(final BatchBuilder builder) {
//        builder.addService(null, new ModuleDeploymentService(this)).addDependency();
    }

    public ModuleIdentifier getIdentifier() {
        return identifier;
    }

    public Dependency[] getDependencies() {
        return dependencies.clone();
    }

    public Resource[] getResources() {
        return resources.clone();
    }

    public static final class Dependency implements Serializable {

        private static final long serialVersionUID = 2749276798703740853L;

        private final ModuleIdentifier identifier;
        private final boolean optional;
        // todo - add import/export filtering, etc.

        public Dependency(final ModuleIdentifier identifier, final boolean optional) {
            this.identifier = identifier;
            this.optional = optional;
        }

        public ModuleIdentifier getIdentifier() {
            return identifier;
        }

        public boolean isOptional() {
            return optional;
        }
    }

    public static final class Resource implements Serializable {

        private static final long serialVersionUID = 3458831155403388498L;

//        private final VirtualFile root;
    }
}
