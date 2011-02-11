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

package org.jboss.as.ee.structure;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.module.ResourceRoot;

/**
 * Marker class used to mark a resource roots that form part of an ears /lib directory.
 * <p>
 * This may also mark resource outside the /lib directory, if they are referenced from the /lib directory with a class-path
 * entry
 *
 * @author Stuart Douglas
 */
public class EarLibResourceMarker {
    private EarLibResourceMarker() {
    }

    public static void markResource(final ResourceRoot context) {
        context.putAttachment(Attachments.EAR_LIB_RESOURCE_MARKER, true);
    }


    public static boolean isEarLibResource(final ResourceRoot context) {
        final Boolean result = context.getAttachment(Attachments.EAR_LIB_RESOURCE_MARKER);
        return result != null && result.booleanValue();
    }
}
