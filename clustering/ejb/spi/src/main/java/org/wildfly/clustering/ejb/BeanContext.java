/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb;

import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;

/**
 * Specifies the deployment/environmental context of a bean.
 *
 * @author Paul Ferraro
 */
public interface BeanContext {
    /**
     * Returns the name of the bean..
     * @return a bean name
     */
    String getBeanName();

    /**
     * Returns the service name of the deployment unit to which this bean is associated.
     * @return a service name
     */
    ServiceName getDeploymentUnitServiceName();

    /**
     * Returns the module loader of this bean's deployment module.
     * @return
     */
    ModuleLoader getModuleLoader();

    /**
     * Returns the class loader of this bean's deployment module.
     * @return
     */
    ClassLoader getClassLoader();

    /**
     * Returns the duration of time this bean can be idle after which it will expire.
     * @return the timeout of this bean
     */
    Time getTimeout();
}
