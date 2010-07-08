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

package org.jboss.as.deployment.descriptor;

import org.jboss.as.deployment.AttachmentKey;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.vfs.VirtualFile;

import java.io.Serializable;

/**
 * A config object capturing the required information to construct a module.
 *
 * @author John E. Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleConfig implements Serializable {

    public static final AttachmentKey<ModuleConfig> ATTACHMENT_KEY = new AttachmentKey<ModuleConfig>(ModuleConfig.class);

    private static final long serialVersionUID = 210753378958448029L;

    private final ModuleIdentifier identifier;
    private final Dependency[] dependencies;
    private final ResourceRoot[] resources;

    public ModuleConfig(final ModuleIdentifier identifier, final Dependency[] dependencies, final ResourceRoot[] resources) {
        this.identifier = identifier;
        this.dependencies = dependencies;
        this.resources = resources;
    }

    public ModuleIdentifier getIdentifier() {
        return identifier;
    }

    public Dependency[] getDependencies() {
        return dependencies;
    }

    public ResourceRoot[] getResources() {
        return resources;
    }

    public static final class Dependency implements Serializable {

        private static final long serialVersionUID = 2749276798703740853L;

        private final ModuleIdentifier identifier;
        private final boolean export;
        private final boolean optional;
        private final boolean staticModule;

        // todo - add import/export filtering, etc.

        public Dependency(final ModuleIdentifier identifier, final boolean staticModule, final boolean optional, final boolean export) {
            this.identifier = identifier;
            this.staticModule = staticModule;
            this.optional = optional;
            this.export = export;
        }

        public ModuleIdentifier getIdentifier() {
            return identifier;
        }

        public boolean isOptional() {
            return optional;
        }

        public boolean isExport() {
            return export;
        }

        public boolean isStatic() {
            return staticModule;
        }
    }

    public static final class ResourceRoot implements Serializable {

        private static final long serialVersionUID = 3458831155403388498L;

        private final String rootName;
        private final VirtualFile root;

        public ResourceRoot(final VirtualFile root) {
            this(root.getName(), root);
        }

        public ResourceRoot(final String rootName, final VirtualFile root) {
            this.rootName = rootName;
            this.root = root;
        }

        public String getRootName() {
            return rootName;
        }

        public VirtualFile getRoot() {
            return root;
        }
    }
}
