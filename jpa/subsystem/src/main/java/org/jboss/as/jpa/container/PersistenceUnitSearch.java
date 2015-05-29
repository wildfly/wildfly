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

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.util.List;

import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.vfs.VirtualFile;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

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
        if (traceEnabled) {
            ROOT_LOGGER.tracef("pu findWithinApplication for %s", persistenceUnitName);
        }

        PersistenceUnitMetadata name = findWithinDeployment(unit, persistenceUnitName);
        if (name != null) {
            if (traceEnabled) {
                ROOT_LOGGER.tracef("pu findWithinApplication matched for %s", persistenceUnitName);
            }
            return name;
        }

        List<ResourceRoot> resourceRoots = unit.getAttachmentList(Attachments.RESOURCE_ROOTS);
        for (ResourceRoot resourceRoot : resourceRoots) {
            if (!SubDeploymentMarker.isSubDeployment(resourceRoot)) {
                name = findWithinLibraryJar(unit, resourceRoot, persistenceUnitName);
                if (name != null) {
                    return name;
                }
            }
        }

        return null;
    }

    private static PersistenceUnitMetadata findWithinLibraryJar(DeploymentUnit unit, ResourceRoot moduleResourceRoot, String persistenceUnitName) {

        final ResourceRoot deploymentRoot = moduleResourceRoot;
        PersistenceUnitMetadataHolder holder = deploymentRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
        if (holder == null || holder.getPersistenceUnits() == null) {
            if (traceEnabled) {
                ROOT_LOGGER.tracef("findWithinLibraryJar checking for '%s' found no persistence units", persistenceUnitName);
            }
            return null;
        }

        ambiguousPUError(unit, persistenceUnitName, holder);
        persistenceUnitName = defaultPersistenceUnitName(persistenceUnitName, holder);

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
        if (traceEnabled) {
            ROOT_LOGGER.tracef("pu findWithinDeployment searching for %s", persistenceUnitName);
        }

        for (ResourceRoot root : DeploymentUtils.allResourceRoots(unit)) {
            PersistenceUnitMetadataHolder holder = root.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
            if (holder == null || holder.getPersistenceUnits() == null) {
                if (traceEnabled) {
                    ROOT_LOGGER.tracef("pu findWithinDeployment skipping empty pu holder for %s", persistenceUnitName);
                }
                continue;
            }

            ambiguousPUError(unit, persistenceUnitName, holder);
            persistenceUnitName = defaultPersistenceUnitName(persistenceUnitName, holder);

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

    private static void ambiguousPUError(DeploymentUnit unit, String persistenceUnitName, PersistenceUnitMetadataHolder holder) {
        if (holder.getPersistenceUnits().size() > 1 &&  (persistenceUnitName == null || persistenceUnitName.length() == 0)) {
            int numberOfDefaultPersistenceUnits = 0;

            // get number of persistence units that are marked as default
            for (PersistenceUnitMetadata persistenceUnit : holder.getPersistenceUnits()) {
                String defaultPU = persistenceUnit.getProperties().getProperty(Configuration.JPA_DEFAULT_PERSISTENCE_UNIT);
                if(Boolean.TRUE.toString().equals(defaultPU)) {
                    numberOfDefaultPersistenceUnits++;
                }
            }
            ROOT_LOGGER.tracef("checking for ambiguous persistence unit injection error, " +
                    "number of persistence units marked default (%s) = %d", Configuration.JPA_DEFAULT_PERSISTENCE_UNIT,  numberOfDefaultPersistenceUnits);
            // don't throw an error if there is exactly one default persistence unit
            if (numberOfDefaultPersistenceUnits != 1) {
                // AS7-2275 no unitName and there is more than one persistence unit;
                throw JpaLogger.ROOT_LOGGER.noPUnitNameSpecifiedAndMultiplePersistenceUnits(holder.getPersistenceUnits().size(), unit);
            }
        }
    }

    /**
     * if no persistence unit name is specified, return name of default persistence unit
     *
     * @param persistenceUnitName that was specified to be used (null means to use the default persistence unit)
     * @param holder
     * @return
     */
    private static String defaultPersistenceUnitName(String persistenceUnitName, PersistenceUnitMetadataHolder holder) {
        if ((persistenceUnitName == null || persistenceUnitName.length() == 0)) {
            for (PersistenceUnitMetadata persistenceUnit : holder.getPersistenceUnits()) {
                String defaultPU = persistenceUnit.getProperties().getProperty(Configuration.JPA_DEFAULT_PERSISTENCE_UNIT);
                if(Boolean.TRUE.toString().equals(defaultPU)) {
                    persistenceUnitName = persistenceUnit.getPersistenceUnitName();
                }
            }
        }
        return persistenceUnitName;
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


        throw JpaLogger.ROOT_LOGGER.persistenceUnitNotFound(absolutePath, puName, current);
    }
}

