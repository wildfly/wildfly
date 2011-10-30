/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.component.entity;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.BasicComponentCreateService;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentCreateServiceFactory;
import org.jboss.as.ejb3.component.EJBComponentCreateService;
import org.jboss.as.ejb3.component.EJBComponentCreateServiceFactory;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.metadata.ejb.spec.EntityBeanMetaData;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;

/**
 * @author Stuart Douglas
 */
public class EntityBeanComponentCreateService extends EJBComponentCreateService {

    private final Class<EJBHome> homeClass;
    private final Class<EJBLocalHome> localHomeClass;
    private final Class<EJBObject> remoteClass;
    private final Class<EJBLocalObject> localClass;
    private final Class<Object> primaryKeyClass;

    public EntityBeanComponentCreateService(final ComponentConfiguration componentConfiguration, final ApplicationExceptions ejbJarConfiguration) {
        super(componentConfiguration, ejbJarConfiguration);
        final EntityBeanComponentDescription description = EntityBeanComponentDescription.class.cast(componentConfiguration.getComponentDescription());
        final EntityBeanMetaData beanMetaData = EntityBeanMetaData.class.cast(description.getDescriptorData());

        final ClassLoader classLoader = componentConfiguration.getComponentClass().getClassLoader();

        homeClass = (Class<EJBHome>) load(classLoader, beanMetaData.getHome());
        localHomeClass = (Class<EJBLocalHome>) load(classLoader, beanMetaData.getLocalHome());
        localClass = (Class<EJBLocalObject>) load(classLoader, beanMetaData.getLocal());
        remoteClass = (Class<EJBObject>) load(classLoader, beanMetaData.getRemote());
        primaryKeyClass = (Class<Object>) load(classLoader, beanMetaData.getPrimKeyClass());
    }

    private Class<?> load(ClassLoader classLoader, String ejbClass) {
        if(ejbClass != null) {
            try {
                return classLoader.loadClass(ejbClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load component view class: " + ejbClass, e);
            }
        }
        return null;
    }

    @Override
    protected BasicComponent createComponent() {
        return new EntityBeanComponent(this);
    }

    public static final ComponentCreateServiceFactory FACTORY = new EJBComponentCreateServiceFactory() {
        @Override
        public BasicComponentCreateService constructService(final ComponentConfiguration configuration) {
            return new EntityBeanComponentCreateService(configuration, this.ejbJarConfiguration);
        }
    };

    public Class<EJBHome> getHomeClass() {
        return homeClass;
    }

    public Class<EJBLocalHome> getLocalHomeClass() {
        return localHomeClass;
    }

    public Class<EJBObject> getRemoteClass() {
        return remoteClass;
    }

    public Class<EJBLocalObject> getLocalClass() {
        return localClass;
    }

    public Class<Object> getPrimaryKeyClass() {
        return primaryKeyClass;
    }
}
