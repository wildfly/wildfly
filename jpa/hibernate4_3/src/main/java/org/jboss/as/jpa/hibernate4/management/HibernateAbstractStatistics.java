/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import org.jipijapa.management.spi.EntityManagerFactoryAccess;
import org.jipijapa.management.spi.Operation;
import org.jipijapa.management.spi.PathAddress;
import org.jipijapa.management.spi.StatisticName;
import org.jipijapa.management.spi.Statistics;

/**
 * HibernateAbstractStatistics
 *
 * @author Scott Marlow
 */
public abstract class HibernateAbstractStatistics implements Statistics {

    private static final String RESOURCE_BUNDLE = HibernateAbstractStatistics.class.getPackage().getName() + ".LocalDescriptions";
    private static final String RESOURCE_BUNDLE_KEY_PREFIX = "hibernate";
    protected Map<String,Operation> operations = new HashMap<String, Operation>();
    protected Set<String> childrenNames = new HashSet<String>();
    protected Set<String> writeableNames = new HashSet<String>();
    protected Map<String, Class> types = new HashMap<String, Class>();
    protected Map<Locale, ResourceBundle> rbs = new HashMap<Locale, ResourceBundle>();

    @Override
    public String getResourceBundleName() {
        return RESOURCE_BUNDLE;
    }

    @Override
    public String getResourceBundleKeyPrefix() {
        return RESOURCE_BUNDLE_KEY_PREFIX;
    }

    protected EntityManagerFactory getEntityManagerFactory(Object[] args) {
        PathAddress pathAddress = getPathAddress(args);
        for(Object arg :args) {
            if (arg instanceof EntityManagerFactoryAccess) {
                EntityManagerFactoryAccess entityManagerFactoryAccess = (EntityManagerFactoryAccess)arg;
                return entityManagerFactoryAccess.entityManagerFactory(pathAddress.getValue(HibernateStatistics.PROVIDER_LABEL));
            }
        }
        return null;
    }

    @Override
    public Set<String> getNames() {
        return Collections.unmodifiableSet(operations.keySet());
    }

    @Override
    public Class getType(String name) {
        return types.get(name);
    }

    @Override
    public boolean isOperation(String name) {
        return Operation.class.equals(getType(name));
    }

    @Override
    public boolean isAttribute(String name) {
        return ! isOperation(name);
    }

    @Override
    public boolean isWriteable(String name) {
        return writeableNames.contains(name);
    }

    @Override
    public Object getValue(String name, EntityManagerFactoryAccess entityManagerFactoryAccess, StatisticName statisticName, PathAddress pathAddress) {
        return operations.get(name).invoke(entityManagerFactoryAccess, statisticName, pathAddress);
    }

    @Override
    public void setValue(String name, Object newValue, EntityManagerFactoryAccess entityManagerFactoryAccess, StatisticName statisticName, PathAddress pathAddress) {
        operations.get(name).invoke(newValue, entityManagerFactoryAccess, statisticName, pathAddress);
    }

    protected EntityManagerFactoryAccess getEntityManagerFactoryAccess(Object[] args) {
        for(Object arg :args) {
            if (arg instanceof EntityManagerFactoryAccess) {
                EntityManagerFactoryAccess entityManagerFactoryAccess = (EntityManagerFactoryAccess)arg;
                return entityManagerFactoryAccess;
            }
        }
        return null;
    }

    protected PathAddress getPathAddress(Object[] args) {
        for(Object arg :args) {
            if (arg instanceof PathAddress) {
                return (PathAddress)arg;
            }
        }
        return null;
    }

    protected String getStatisticName(Object[] args) {
        for(Object arg :args) {
            if (arg instanceof StatisticName) {
                StatisticName name = (StatisticName)arg;
                return name.getName();
            }
        }
        return null;
    }

    @Override
    public Set<String> getChildrenNames() {
        return Collections.unmodifiableSet(childrenNames);
    }

    @Override
    public Statistics getChild(String childName) {
        return null;
    }
}
