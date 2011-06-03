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

package org.jboss.as.osgi.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.osgi.framework.launch.Framework;

/**
 * Utility to help attach and retrieve a Framework from a deployment context.
 *
 * @author Thomas.Diesler@jboss.com
 */
public class FrameworkAttachment {
    public static final AttachmentKey<Framework> KEY = AttachmentKey.create(Framework.class);

    public static void attachFramework(final DeploymentUnit depUnit, final Framework metadata) {
        depUnit.putAttachment(KEY, metadata);
    }

    public static Framework getFramework(final DeploymentUnit depUnit) {
        return depUnit.getAttachment(KEY);
    }

    public static void detachFramework(final DeploymentUnit depUnit) {
        depUnit.removeAttachment(KEY);
    }
}
