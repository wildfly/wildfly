/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.container;

import org.jboss.as.jpa.config.PersistenceUnitMetadata;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Logger;
import org.jboss.vfs.VirtualFile;

import java.util.List;

/**
 * Perform scoped search for persistence unit name
 *
 * @author Scott Marlow (forked from Carlo de Wolf code).
 * @author Stuart Douglas
 */
public class PersistenceUnitSearch {
    private static final Logger log = Logger.getLogger("org.jboss.jpa");
    // cache the trace enabled flag
    private static final boolean traceEnabled = log.isTraceEnabled();

    public static PersistenceUnitMetadata resolvePersistenceUnitSupplier(DeploymentUnit deploymentUnit, String persistenceUnitName) {
        if (traceEnabled) {
            log.tracef("pu search for name '%s' inside of %s", persistenceUnitName, deploymentUnit.getName());
        }
        int i = (persistenceUnitName == null ? -1 : persistenceUnitName.indexOf('#'));
        if (i != -1) {
            final String path = persistenceUnitName.substring(0, i);
            final String name = persistenceUnitName.substring(i + 1);
            PersistenceUnitMetadata pu = getPersistenceUnit(deploymentUnit, path, name);
            if (traceEnabled) {
                log.trace("pu search found " + pu.getScopedPersistenceUnitName());
            }
            return pu;
        } else {
            PersistenceUnitMetadata name = findPersistenceUnitSupplier(deploymentUnit, persistenceUnitName);
            if (traceEnabled) {
                if (name != null) {
                    log.trace("pu search found " + name);
                }
            }
            return name;
        }
    }

    private static PersistenceUnitMetadata findPersistenceUnitSupplier(DeploymentUnit deploymentUnit, String persistenceUnitName) {
        PersistenceUnitMetadata name = findWithinDeployment(deploymentUnit, persistenceUnitName);
        if (name == null) {
            name = findWithinApplication(getTopLevel(deploymentUnit), persistenceUnitName);
        }
        return name;
    }

    private static PersistenceUnitMetadata findWithinApplication(DeploymentUnit unit, String persistenceUnitName) {
        PersistenceUnitMetadata name = findWithinDeployment(unit, persistenceUnitName);
        if (name != null) {
            return name;
        }

        List<ResourceRoot> resourceRoots = unit.getAttachmentList(Attachments.RESOURCE_ROOTS);

        for (ResourceRoot resourceRoot : resourceRoots) {
            if (!SubDeploymentMarker.isSubDeployment(resourceRoot)) {
                name = findWithinLibraryJar(resourceRoot, persistenceUnitName);
                if (name != null) {
                    return name;
                }
            }
        }

        return null;
    }

    private static PersistenceUnitMetadata findWithinLibraryJar(ResourceRoot moduleResourceRoot, String persistenceUnitName) {

        final ResourceRoot deploymentRoot = moduleResourceRoot;
        PersistenceUnitMetadataHolder holder = deploymentRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
        if (holder == null || holder.getPersistenceUnits() == null)
            return null;

        for (PersistenceUnitMetadata persistenceUnit : holder.getPersistenceUnits()) {
            if (traceEnabled) {
                log.tracef("findWithinLibraryJar check '%s' against pu '%s'", persistenceUnitName, persistenceUnit.getPersistenceUnitName());
            }
            if (persistenceUnitName == null || persistenceUnitName.length() == 0 || persistenceUnit.getPersistenceUnitName().equals(persistenceUnitName)) {
                if (traceEnabled) {
                    log.tracef("findWithinLibraryJar matched '%s' against pu '%s'", persistenceUnitName, persistenceUnit.getPersistenceUnitName());
                }
                return persistenceUnit;
            }
        }
        return null;
    }

    /*
     * When finding the default persistence unit, the first persistence unit encountered is returned.
     */
    private static PersistenceUnitMetadata findWithinDeployment(DeploymentUnit unit, String persistenceUnitName) {

        final ResourceRoot deploymentRoot = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        PersistenceUnitMetadataHolder holder = deploymentRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
        if (holder == null || holder.getPersistenceUnits() == null)
            return null;

        for (PersistenceUnitMetadata persistenceUnit : holder.getPersistenceUnits()) {
            if (traceEnabled) {
                log.tracef("findWithinDeployment check '%s' against pu '%s'", persistenceUnitName, persistenceUnit.getPersistenceUnitName());
            }
            if (persistenceUnitName == null || persistenceUnitName.length() == 0 || persistenceUnit.getPersistenceUnitName().equals(persistenceUnitName)) {
                if (traceEnabled) {
                    log.tracef("findWithinDeployment matched '%s' against pu '%s'", persistenceUnitName, persistenceUnit.getPersistenceUnitName());
                }
                return persistenceUnit;
            }
        }
        return null;
    }

    private static DeploymentUnit getTopLevel(DeploymentUnit du) {
        return du.getParent() == null ? du : du.getParent();
    }

    private static PersistenceUnitMetadata getPersistenceUnit(DeploymentUnit current, final String absolutePath, String puName) {
        final String path;
        if (absolutePath.startsWith("../")) {
            path = absolutePath.substring(3);
        } else {
            path = absolutePath;
        }
        final VirtualFile parent = current.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot().getParent();
        final VirtualFile resolvedPath = parent.getChild(path);

        List<ResourceRoot> resourceRoots = DeploymentUtils.allResourceRoots(getTopLevel(current));

        for (ResourceRoot resourceRoot : resourceRoots) {
            if (resourceRoot.getRoot().equals(resolvedPath)) {
                PersistenceUnitMetadataHolder holder = resourceRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
                if (holder != null) {
                    for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                        if (traceEnabled) {
                            log.tracef("getPersistenceUnit check '%s' against pu '%s'", puName, pu.getPersistenceUnitName());
                        }
                        if (pu.getPersistenceUnitName().equals(puName)) {
                            if (traceEnabled) {
                                log.tracef("getPersistenceUnit matched '%s' against pu '%s'", puName, pu.getPersistenceUnitName());
                            }
                            return pu;
                        }
                    }
                }
            }
        }


        throw new IllegalArgumentException("Can't find a deployment unit named " + absolutePath + "#" + puName + " at " + current);
    }
}

