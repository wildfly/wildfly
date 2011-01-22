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

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.jpa.config.PersistenceUnitMetadata;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;

import java.util.List;

/**
 * Perform scoped search for persistence unit name
 *
 * @author Scott Marlow (forked from Carlo de Wolf code).
 */
public class PersistenceUnitSearch {


    public static String resolvePersistenceUnitSupplier(DeploymentUnit deploymentUnit, String persistenceUnitName) {
        int i = (persistenceUnitName == null ? -1 : persistenceUnitName.indexOf('#'));
        if (i != -1) {
            String path = persistenceUnitName.substring(0, i);
            PersistenceUnitMetadata pu = getPersistenceUnit(deploymentUnit, path);
            return pu.getScopedPersistenceUnitName();
        } else {
            String name = findPersistenceUnitSupplier(deploymentUnit, persistenceUnitName);
            if (name == null)
                throw new IllegalArgumentException("Can't find a persistence unit named '" + persistenceUnitName + "' in " + deploymentUnit);
            return name;
        }
    }

    private static String findPersistenceUnitSupplier(DeploymentUnit deploymentUnit, String persistenceUnitName) {
        String name = findWithinModule(deploymentUnit, persistenceUnitName, true);
        if (name == null)
            name = findWithinApplication(getTopLevel(deploymentUnit), persistenceUnitName);
        return name;
    }

    private static String findWithinApplication(DeploymentUnit unit, String persistenceUnitName) {
        String name = findWithinModule(unit, persistenceUnitName, false);
        if (name != null)
            return name;

        List<ResourceRoot> resourceRoots = unit.getAttachmentList(Attachments.RESOURCE_ROOTS);

        for (ResourceRoot resourceRoot : resourceRoots) {
            name = findWithinApplication(resourceRoot, persistenceUnitName);
            if (name != null)
                return name;
        }

        return null;
    }

    private static String findWithinApplication(ResourceRoot moduleResourceRoot, String persistenceUnitName) {
        String name = findWithinModule(moduleResourceRoot, persistenceUnitName, false);
        if (name != null)
            return name;

        List<ResourceRoot> resourceRoots = moduleResourceRoot.getAttachmentList(Attachments.RESOURCE_ROOTS);

        for (ResourceRoot resourceRoot : resourceRoots) {
            name = findWithinApplication(resourceRoot, persistenceUnitName);
            if (name != null)
                return name;
        }

        return null;
    }


    private static String findWithinModule(ResourceRoot moduleResourceRoot, String persistenceUnitName, boolean allowScoped) {


        if (!allowScoped && isScoped(moduleResourceRoot))
            return null;

        final ResourceRoot deploymentRoot = moduleResourceRoot;
        PersistenceUnitMetadataHolder holder = deploymentRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
        if (holder == null || holder.getPersistenceUnits() == null)
            return null;

        for (PersistenceUnitMetadata persistenceUnit : holder.getPersistenceUnits()) {
            if (persistenceUnitName == null || persistenceUnitName.length() == 0 || persistenceUnit.getPersistenceUnitName().equals(persistenceUnitName))
                return persistenceUnit.getScopedPersistenceUnitName();
        }
        return null;
    }

    /*
     * When finding the default persistence unit, the first persistence unit encountered is returned.
     * TODO: Maybe the name of unscoped persistence units should be changed, so only one can be deployed anyway.
     */
    private static String findWithinModule(DeploymentUnit unit, String persistenceUnitName, boolean allowScoped) {
        if (!allowScoped && isScoped(unit))
            return null;

        final ResourceRoot deploymentRoot = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        PersistenceUnitMetadataHolder holder = deploymentRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
        if (holder == null || holder.getPersistenceUnits() == null)
            return null;

        for (PersistenceUnitMetadata persistenceUnit : holder.getPersistenceUnits()) {
            if (persistenceUnitName == null || persistenceUnitName.length() == 0 || persistenceUnit.getPersistenceUnitName().equals(persistenceUnitName))
                return persistenceUnit.getScopedPersistenceUnitName();
        }

        return null;
    }

    /*
    * JPA 6.2.2: Persistence Unit Scope
    */
    private static boolean isScoped(DeploymentUnit unit) {

        // TODO:  add CLIENT
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, unit) ||
            DeploymentTypeMarker.isType(DeploymentType.WAR, unit))
            return true;
        return false;
    }

    private static boolean isScoped(ResourceRoot moduleResourceRoot) {

        if (moduleResourceRoot.getRoot().getLowerCaseName().endsWith(".ear") ||
            moduleResourceRoot.getRoot().getLowerCaseName().endsWith(".war")) {
            return true;
        }
        return false;
    }

    private static DeploymentUnit getTopLevel(DeploymentUnit du) {
        while (du.getParent() != null) {
            du = du.getParent();
        }
        return du;
    }

    private static PersistenceUnitMetadata getPersistenceUnit(DeploymentUnit current, String path) {
        if (path.startsWith("/"))
            return getPersistenceUnit(getTopLevel(current), path.substring(1));
        if (path.startsWith("./"))
            return getPersistenceUnit(current, path.substring(2));
        if (path.startsWith("../"))
            return getPersistenceUnit(current.getParent(), path.substring(3));
        int i = path.indexOf('/');
        String name;
        if (i == -1)
            name = path;
        else
            name = path.substring(0, i);

        List<ResourceRoot> resourceRoots = current.getAttachmentList(Attachments.RESOURCE_ROOTS);

        for (ResourceRoot resourceRoot : resourceRoots) {
            PersistenceUnitMetadataHolder holder = resourceRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
            if (holder != null) {
                for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                    if (pu.getPersistenceUnitName().equals(name)) {
                        return pu;
                    }
                }
            }
        }


        throw new IllegalArgumentException("Can't find a deployment unit named " + name + " at " + current);
    }
}

