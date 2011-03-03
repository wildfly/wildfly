/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.AbstractComponent;
import org.jboss.as.ee.component.AbstractComponentInstance;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.ejb3.effigy.common.JBossSessionBeanEffigy;
import org.jboss.invocation.Interceptor;

import javax.annotation.Resource;

/**
 * {@link org.jboss.as.ee.component.Component} responsible for managing EJB3 stateless session beans
 * <p/>
 * <p/>
 * Author : Jaikiran Pai
 */
public class StatelessEJBComponent extends AbstractComponent {


    // TODO: Need to use the right "name" for the @Resource
    @Resource
    private JBossSessionBeanEffigy sessionBeanEffigy;

    // some more injectable resources
    // @Resource
    // private Pool pool;

    /**
     * Construct a new instance.
     *
     * @param configuration         the component configuration
     * @param deploymentClassLoader the class loader of the deployment
     * @param index                 the deployment reflection index
     */
    protected StatelessEJBComponent(final ComponentConfiguration configuration, final ClassLoader deploymentClassLoader, final DeploymentReflectionIndex index) {
        super(configuration, deploymentClassLoader, index);
    }

    @Override
    protected AbstractComponentInstance constructComponentInstance(Object instance) {
        throw new RuntimeException("NYI: org.jboss.as.ejb3.component.StatelessEJBComponent.constructComponentInstance");
    }

    @Override
    public Interceptor createClientInterceptor(Class<?> view) {
        throw new RuntimeException("NYI: org.jboss.as.ejb3.component.StatelessEJBComponent.createClientInterceptor");
    }
}
