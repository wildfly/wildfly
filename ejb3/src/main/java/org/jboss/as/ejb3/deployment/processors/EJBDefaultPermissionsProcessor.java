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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.security.ImmediatePermissionFactory;
import org.jboss.modules.security.PermissionFactory;

import java.io.FilePermission;
import java.net.SocketPermission;
import java.security.Permission;
import java.security.Permissions;
import java.util.Enumeration;
import java.util.List;
import java.util.PropertyPermission;

/**
 * A processor which sets up the default Java EJB permission set.
 *
 * This can be disabled via config
 *
 * @author Stuart Douglas
 */
public final class EJBDefaultPermissionsProcessor implements DeploymentUnitProcessor {
    private static final Permissions DEFAULT_PERMISSIONS;

    private volatile boolean enabled;

    static {
        final Permissions permissions = new Permissions();
        permissions.add(new PropertyPermission("*", "read"));
        permissions.add(new RuntimePermission("queuePrintJob"));
        permissions.add(new RuntimePermission("loadLibrary"));
        permissions.add(new FilePermission("*", "read"));
        permissions.add(new FilePermission("*", "write"));
        permissions.add(new SocketPermission("*", "connect"));
        DEFAULT_PERMISSIONS = permissions;
    }

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final ModuleSpecification attachment = phaseContext.getDeploymentUnit().getAttachment(Attachments.MODULE_SPECIFICATION);
        if (attachment == null) {
            return;
        }
        if(!enabled) {
            return;
        }
        if (!EjbDeploymentMarker.isEjbDeployment(phaseContext.getDeploymentUnit())) {
            return;
        }
        final List<PermissionFactory> permissions = attachment.getPermissionFactories();
        final Enumeration<Permission> e = DEFAULT_PERMISSIONS.elements();
        while (e.hasMoreElements()) {
            permissions.add(new ImmediatePermissionFactory(e.nextElement()));
        }
    }

    public void undeploy(final DeploymentUnit context) {
        // no op
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
