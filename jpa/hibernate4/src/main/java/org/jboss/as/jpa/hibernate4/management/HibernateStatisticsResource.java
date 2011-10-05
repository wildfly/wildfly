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

package org.jboss.as.jpa.hibernate4.management;

import static org.jboss.as.jpa.hibernate4.management.HibernateDescriptionConstants.CACHE;

import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.hibernate.stat.Statistics;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jpa.spi.PersistenceUnitService;
import org.jboss.as.jpa.spi.PersistenceUnitServiceRegistry;
import org.jboss.dmr.ModelNode;

/**
 * Resource representing a JPA PersistenceUnit (from a persistence.xml) deployment.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HibernateStatisticsResource extends PlaceholderResource.PlaceholderResourceEntry {

    private final String puName;
    private final PersistenceUnitServiceRegistry persistenceUnitRegistry;
    private final ModelNode model = new ModelNode();
    public HibernateStatisticsResource(final String puName, final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        super(HibernateDescriptionConstants.CACHE, puName);
        this.puName = puName;
        this.persistenceUnitRegistry = persistenceUnitRegistry;
    }

    @Override
    public ModelNode getModel() {
        return model;
    }

    @Override
    public boolean isModelDefined() {
        return model.isDefined();
    }

    @Override
    public boolean hasChild(PathElement element) {
        if (CACHE.equals(element.getKey())) {
            return hasCacheRegion(element);
        } else {
            return super.hasChild(element);
        }
    }

    @Override
    public Resource getChild(PathElement element) {
        if (CACHE.equals(element.getKey())) {
            return hasCacheRegion(element) ? PlaceholderResource.INSTANCE : null;
        } else {
            return super.getChild(element);
        }
    }

    @Override
    public Resource requireChild(PathElement element) {
        if (CACHE.equals(element.getKey())) {
            if (hasCacheRegion(element)) {
                return PlaceholderResource.INSTANCE;
            }
            throw new NoSuchElementException(element.toString());
        } else {
            return super.requireChild(element);
        }
    }

    @Override
    public boolean hasChildren(String childType) {
        if (CACHE.equals(childType)) {
            return getChildrenNames(CACHE).size() > 0;
        } else {
            return super.hasChildren(childType);
        }
    }

    @Override
    public Resource navigate(PathAddress address) {
        if (address.size() > 0 && CACHE.equals(address.getElement(0).getKey())) {
            if (address.size() > 1) {
                throw new NoSuchElementException(address.subAddress(1).toString());
            }
            return PlaceholderResource.INSTANCE;
        } else {
            return super.navigate(address);
        }
    }

    @Override
    public Set<String> getChildTypes() {
        Set<String> result = new HashSet<String>(super.getChildTypes());
        result.add(CACHE);
        return result;
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (CACHE.equals(childType)) {
            return getCacheRegionNames();
        } else {
            return super.getChildrenNames(childType);
        }
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (CACHE.equals(childType)) {
            Set<ResourceEntry> result = new HashSet<ResourceEntry>();
            for (String name : getCacheRegionNames()) {
                result.add(new PlaceholderResource.PlaceholderResourceEntry(CACHE, name));
            }
            return result;
        } else {
            return super.getChildren(childType);
        }
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        if (CACHE.equals(address.getKey())) {
            throw new UnsupportedOperationException(String.format("Resources of type %s cannot be registered", CACHE));
        } else {
            super.registerChild(address, resource);
        }
    }

    @Override
    public Resource removeChild(PathElement address) {
        if (CACHE.equals(address.getKey())) {
            throw new UnsupportedOperationException(String.format("Resources of type %s cannot be removed", CACHE));
        } else {
            return super.removeChild(address);
        }
    }

    @Override
    public boolean isRuntime() {
        return false;
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public HibernateStatisticsResource clone() {
        return new HibernateStatisticsResource(puName, persistenceUnitRegistry);
    }

    private boolean hasCacheRegion(PathElement element) {
        boolean result = false;
        final PersistenceUnitService puService = persistenceUnitRegistry.getPersistenceUnitService(puName);
        final Statistics stats = getStatistics();
        if (stats != null && puService != null) {
            final String scopedPUName = puService.getScopedPersistenceUnitName();
            final String unqualifiedRegionName = element.getValue();
            final String qualifiedRegionName = scopedPUName + "." + unqualifiedRegionName;
            result = stats.getSecondLevelCacheStatistics(qualifiedRegionName) != null;
        }
        return result;
    }

    private Set<String> getCacheRegionNames() {
        final Statistics stats = getStatistics();
        if (stats == null) {
            return Collections.emptySet();
        } else {
            Set<String> result = new HashSet<String>();
            String[] cacheRegionNames = stats.getSecondLevelCacheRegionNames();
            if (cacheRegionNames != null) {
                for (String region : cacheRegionNames) {

                    // example regionName = "jpa_SecondLevelCacheTestCase.jar#mypc.org.jboss.as.testsuite.integration.jpa.hibernate.Employee"
                    // remove the scoped PU name plus one for '.' the separator character added to it.
                    // and replace period with underscore.  Filtered region name will be "org_jboss_as_testsuite_integration_jpa_hibernate_Employee"
                    int stripUpTo = puName.length() + 1;
                    result.add(region.substring(stripUpTo));
                }
            }
            return result;
        }
    }

    private Statistics getStatistics() {
        return ManagementUtility.getStatistics(persistenceUnitRegistry, puName);
    }
}
