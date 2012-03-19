/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jsr88.spi;

import javax.enterprise.deploy.model.DDBeanRoot;
import javax.enterprise.deploy.spi.DConfigBean;
import javax.enterprise.deploy.spi.DConfigBeanRoot;

/**
 * The DConfigBeanRoot object is a deployment configuration bean (DConfigBean) that is associated with the root of the
 * component's deployment descriptor. It must be created by calling the DeploymentConfiguration.getDConfigBean(DDBeanRoot)
 * method, where DDBeanRoot represents the component's deployment descriptor.
 *
 * @author Thomas.Diesler@jboss.com
 *
 */
class DConfigBeanRootImpl extends DConfigBeanImpl implements DConfigBeanRoot {
    /**
     * Return the configuration that is not the primary deployment descriptor
     *
     * @param beanRoot the root of the deployment descriptor
     * @return the configuration
     */
    public DConfigBean getDConfigBean(DDBeanRoot beanRoot) {
        return null; // [todo] implement method
    }
}
