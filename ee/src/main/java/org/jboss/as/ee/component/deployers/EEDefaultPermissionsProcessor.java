/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component.deployers;

import java.security.Permission;
import java.security.Permissions;
import java.util.Enumeration;
import org.jboss.as.naming.JndiPermission;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleSpecification;

import static org.jboss.as.naming.JndiPermission.Action.LIST;
import static org.jboss.as.naming.JndiPermission.Action.LIST_BINDINGS;
import static org.jboss.as.naming.JndiPermission.Action.LOOKUP;

/**
 * A processor which sets up the default Java EE permission set.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EEDefaultPermissionsProcessor implements DeploymentUnitProcessor {
    private static final Permissions DEFAULT_PERMISSIONS;

    static {
        final Permissions permissions = new Permissions();
        permissions.add(new JndiPermission("java:comp/-", LOOKUP, LIST, LIST_BINDINGS));
        permissions.add(new JndiPermission("java:module/-", LOOKUP, LIST, LIST_BINDINGS));
        permissions.add(new JndiPermission("java:app/-", LOOKUP, LIST, LIST_BINDINGS));
        permissions.add(new JndiPermission("java:global/-", LOOKUP));
        permissions.add(new JndiPermission("java:jboss/-", LOOKUP));
        permissions.add(new JndiPermission("java:/-", LOOKUP));
        DEFAULT_PERMISSIONS = permissions;
    }

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final ModuleSpecification attachment = phaseContext.getAttachment(Attachments.MODULE_SPECIFICATION);
        if (attachment == null) {
            return;
        }
        final Permissions permissions = attachment.getPermissions();
        final Enumeration<Permission> e = DEFAULT_PERMISSIONS.elements();
        while (e.hasMoreElements()) {
            permissions.add(e.nextElement());
        }
    }

    public void undeploy(final DeploymentUnit context) {
        // no op
    }
}
