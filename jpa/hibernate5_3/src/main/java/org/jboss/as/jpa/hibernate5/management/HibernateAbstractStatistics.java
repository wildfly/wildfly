/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jpa.hibernate5.management;

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
