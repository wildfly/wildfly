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

import static org.jboss.as.jpa.JpaLogger.ROOT_LOGGER;
import static org.jboss.as.jpa.JpaMessages.MESSAGES;

import java.util.List;

import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.config.PersistenceUnitsInApplication;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.vfs.VirtualFile;

/**
 * Perform scoped search for persistence unit name
 *
 * @author Scott Marlow (forked from Carlo de Wolf code).
 * @author Stuart Douglas
 */
public class PersistenceUnitSearch {
    // cache the trace enabled flag
    private static final boolean traceEnabled = ROOT_LOGGER.isTraceEnabled();

    public static PersistenceUnitMetadata resolvePersistenceUnitSupplier(DeploymentUnit deploymentUnit, String persistenceUnitName) {
        if (traceEnabled) {
            ROOT_LOGGER.tracef("pu search for name '%s' inside of %s", persistenceUnitName, deploymentUnit.getName());
        }
        int scopeSeparatorCharacter = (persistenceUnitName == null ? -1 : persistenceUnitName.indexOf('#'));
        if (scopeSeparatorCharacter != -1) {
            final String path = persistenceUnitName.substring(0, scopeSeparatorCharacter);
            final String name = persistenceUnitName.substring(scopeSeparatorCharacter + 1);
            PersistenceUnitMetadata pu = getPersistenceUnit(deploymentUnit, path, name);
            if (traceEnabled) {
                ROOT_LOGGER.tracef("pu search found %s", pu.getScopedPersistenceUnitName());
            }
            return pu;
        } else {
            if ( persistenceUnitName == null) {
                PersistenceUnitsInApplication persistenceUnitsInApplication = DeploymentUtils.getTopDeploymentUnit(deploymentUnit).getAttachment(PersistenceUnitsInApplication.PERSISTENCE_UNITS_IN_APPLICATION);

                if (persistenceUnitsInApplication.getCount() > 1) {
                    // AS7-2275 no unitName and there is more than one persistence unit;
                    throw MESSAGES.noPUnitNameSpecifiedAndMultiplePersistenceUnits(persistenceUnitsInApplication.getCount(),DeploymentUtils.getTopDeploymentUnit(deploymentUnit));
                }
                ROOT_LOGGER.tracef("pu searching with empty unit name, application %s has %d persistence unit definitions",
                    DeploymentUtils.getTopDeploymentUnit(deploymentUnit).getName(), persistenceUnitsInApplication.getCount());
            }
            PersistenceUnitMetadata name = findPersistenceUnitSupplier(deploymentUnit, persistenceUnitName);
            if (traceEnabled) {
                if (name != null) {
                    ROOT_LOGGER.tracef("pu search found %s", name.getScopedPersistenceUnitName());
                }
            }
            return name;
        }
    }

    private static PersistenceUnitMetadata findPersistenceUnitSupplier(DeploymentUnit deploymentUnit, String persistenceUnitName) {
        PersistenceUnitMetadata name = findWithinDeployment(deploymentUnit, persistenceUnitName);
        if (name == null) {
            name = findWithinApplication(DeploymentUtils.getTopDeploymentUnit(deploymentUnit), persistenceUnitName);
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
                ROOT_LOGGER.tracef("findWithinLibraryJar check '%s' against pu '%s'", persistenceUnitName, persistenceUnit.getPersistenceUnitName());
            }
            if (persistenceUnitName == null || persistenceUnitName.length() == 0 || persistenceUnit.getPersistenceUnitName().equals(persistenceUnitName)) {
                if (traceEnabled) {
                    ROOT_LOGGER.tracef("findWithinLibraryJar matched '%s' against pu '%s'", persistenceUnitName, persistenceUnit.getPersistenceUnitName());
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

        for (ResourceRoot root : DeploymentUtils.allResourceRoots(unit)) {
            PersistenceUnitMetadataHolder holder = root.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
            if (holder == null || holder.getPersistenceUnits() == null) {
                continue;
            }

            for (PersistenceUnitMetadata persistenceUnit : holder.getPersistenceUnits()) {
                if (traceEnabled) {
                    ROOT_LOGGER.tracef("findWithinDeployment check '%s' against pu '%s'", persistenceUnitName, persistenceUnit.getPersistenceUnitName());
                }
                if (persistenceUnitName == null || persistenceUnitName.length() == 0 || persistenceUnit.getPersistenceUnitName().equals(persistenceUnitName)) {
                    if (traceEnabled) {
                        ROOT_LOGGER.tracef("findWithinDeployment matched '%s' against pu '%s'", persistenceUnitName, persistenceUnit.getPersistenceUnitName());
                    }
                    return persistenceUnit;
                }
            }
        }
        return null;
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

        List<ResourceRoot> resourceRoots = DeploymentUtils.allResourceRoots(DeploymentUtils.getTopDeploymentUnit(current));

        for (ResourceRoot resourceRoot : resourceRoots) {
            if (resourceRoot.getRoot().equals(resolvedPath)) {
                PersistenceUnitMetadataHolder holder = resourceRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
                if (holder != null) {
                    for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                        if (traceEnabled) {
                            ROOT_LOGGER.tracef("getPersistenceUnit check '%s' against pu '%s'", puName, pu.getPersistenceUnitName());
                        }
                        if (pu.getPersistenceUnitName().equals(puName)) {
                            if (traceEnabled) {
                                ROOT_LOGGER.tracef("getPersistenceUnit matched '%s' against pu '%s'", puName, pu.getPersistenceUnitName());
                            }
                            return pu;
                        }
                    }
                }
            }
        }


        throw MESSAGES.deploymentUnitNotFound(absolutePath, puName, current);
    }
}

