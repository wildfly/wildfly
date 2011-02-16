/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

/**
 * Marker for module roots. These are resource roots that are added to the module.
 *
 * @author Stuart Douglas
 *
 */
public class ModuleRootMarker {
    private static final AttachmentKey<Boolean> MODULE_ROOT_MARKER = AttachmentKey.create(Boolean.class);

    public static void mark(ResourceRoot attachable) {
        attachable.putAttachment(MODULE_ROOT_MARKER, true);
    }

    public static void mark(ResourceRoot attachable, boolean value) {
        attachable.putAttachment(MODULE_ROOT_MARKER, value);
    }

    public static boolean isModuleRoot(ResourceRoot resourceRoot) {
        Boolean res = resourceRoot.getAttachment(MODULE_ROOT_MARKER);
        return res != null && res;
    }

    private ModuleRootMarker() {

    }
}
