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

package org.jboss.as.deployment;

import java.util.jar.Manifest;
import org.jboss.as.deployment.module.ModuleDependency;
import org.jboss.as.deployment.module.ResourceRoot;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;
import org.jboss.vfs.VirtualFile;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Attachments {

    //
    // STRUCTURE
    //

    /**
     * The primary deployment root.
     */
    public static final AttachmentKey<ResourceRoot> DEPLOYMENT_ROOT = AttachmentKey.create(ResourceRoot.class);
    /**
     * The additional resource roots of the deployment unit.
     */
    public static final AttachmentKey<AttachmentList<ResourceRoot>> RESOURCE_ROOTS = AttachmentKey.createList(ResourceRoot.class);
    /**
     * The MANIFEST.MF of the deployment unit.
     */
    public static final AttachmentKey<Manifest> MANIFEST = AttachmentKey.create(Manifest.class);
    /**
     * ?????
     */
    public static final AttachmentKey<Manifest> OSGI_MANIFEST = AttachmentKey.create(Manifest.class);
    /**
     * The list of subdeployments detected.
     */
    public static final AttachmentKey<AttachmentList<VirtualFile>> SUBDEPLOYMENT_ROOTS = AttachmentKey.createList(VirtualFile.class);

    //
    // VALIDATE
    //

    //
    // PARSE
    //
    /**
     * The annotation index for this deployment.
     */
    public static final AttachmentKey<Index> ANNOTATION_INDEX = AttachmentKey.create(Index.class);

    //
    // DEPENDENCIES
    //
    /**
     * The list of module dependencies.
     */
    public static final AttachmentKey<AttachmentList<ModuleDependency>> MODULE_DEPENDENCIES = AttachmentKey.createList(ModuleDependency.class);

    //
    // MODULARIZE
    //

    /**
     * The module of this deployment unit.
     */
    public static final AttachmentKey<Module> MODULE = AttachmentKey.create(Module.class);

    //
    // POST_MODULE
    //

    //
    // INSTALL
    //

    //
    // CLEANUP
    //

    private Attachments() {
    }


}
