/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component.deployers;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.security.Permission;
import java.security.Permissions;
import java.util.Enumeration;
import java.util.List;

import org.wildfly.naming.java.permission.JndiPermission;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.security.ImmediatePermissionFactory;
import org.jboss.modules.security.PermissionFactory;

/**
 * A processor which sets up the default Jakarta EE permission set.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EEDefaultPermissionsProcessor implements DeploymentUnitProcessor {
    private static final Permissions DEFAULT_PERMISSIONS;

    static {
        final Permissions permissions = new Permissions();
        final int actionBits = JndiPermission.ACTION_LOOKUP | JndiPermission.ACTION_LIST | JndiPermission.ACTION_LIST_BINDINGS;
        permissions.add(new JndiPermission("java:comp", actionBits));
        permissions.add(new JndiPermission("java:comp/-", actionBits));
        permissions.add(new JndiPermission("java:module", actionBits));
        permissions.add(new JndiPermission("java:module/-", actionBits));
        permissions.add(new JndiPermission("java:app", actionBits));
        permissions.add(new JndiPermission("java:app/-", actionBits));
        permissions.add(new JndiPermission("java:global", JndiPermission.ACTION_LOOKUP));
        permissions.add(new JndiPermission("java:global/-", JndiPermission.ACTION_LOOKUP));
        permissions.add(new JndiPermission("java:jboss", JndiPermission.ACTION_LOOKUP));
        permissions.add(new JndiPermission("java:jboss/-", JndiPermission.ACTION_LOOKUP));
        permissions.add(new JndiPermission("java:/-", JndiPermission.ACTION_LOOKUP));
        DEFAULT_PERMISSIONS = permissions;
    }

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification attachment = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        if (attachment == null) {
            return;
        }
        final List<PermissionFactory> permissions = attachment.getPermissionFactories();
        final Enumeration<Permission> e = DEFAULT_PERMISSIONS.elements();
        while (e.hasMoreElements()) {
            permissions.add(new ImmediatePermissionFactory(e.nextElement()));
        }

        //make sure they can read the contents of the deployment
        ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        try {
            File file = root.getRoot().getPhysicalFile();
            if(file != null && file.isDirectory()) {
                FilePermission permission = new FilePermission(file.getAbsolutePath() + File.separatorChar + "-", "read");
                permissions.add(new ImmediatePermissionFactory(permission));
            }
        } catch (IOException ex) {
            throw new DeploymentUnitProcessingException(ex);
        }

    }
}
