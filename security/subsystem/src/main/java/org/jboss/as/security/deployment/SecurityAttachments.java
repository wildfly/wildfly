/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.security.deployment;

import org.jboss.as.server.deployment.AttachmentKey;

/**
 * @author Stuart Douglas
 */
public class SecurityAttachments {

    /**
     * Attachment key that if present signifies that the security subsystem is installed.
     * <p>
     * If this is not present either the subsystem has been removed from the config or excluded
     * via jboss-deployment-structure.xml. This allows deployments to disable security, and
     * avoid the overhead of running unneeded security code.
     *
     * @deprecated Check the presence of {@link org.jboss.as.security.SecuritySubsystemRootResourceDefinition#SECURITY_SUBSYSTEM}
     * capability instead to avoid classloading dependencies on org.jboss.as.security. If the capability is not in the
     * capability registry, then we can assume that the security subsystem is not configured.
     */
    @Deprecated
    public static final AttachmentKey<Boolean> SECURITY_ENABLED = AttachmentKey.create(Boolean.class);

}
