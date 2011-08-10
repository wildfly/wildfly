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
package org.jboss.as.ee.deployment.spi;

import java.io.InputStream;
import java.io.OutputStream;

import javax.enterprise.deploy.model.DDBeanRoot;
import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.spi.DConfigBeanRoot;
import javax.enterprise.deploy.spi.DeploymentConfiguration;
import javax.enterprise.deploy.spi.exceptions.BeanNotFoundException;
import javax.enterprise.deploy.spi.exceptions.ConfigurationException;

/**
 * An interface that defines a container for all the server-specific configuration information for a single top-level J2EE
 * module. The DeploymentConfiguration object could represent a single stand alone module or an EAR file that contains several
 * sub-modules.
 *
 * @author Thomas.Diesler@jboss.com
 *
 */
class DeploymentConfigurationImpl implements DeploymentConfiguration {
    /**
     * Return an object that provides access to the deployment descriptor
     *
     * @return the deployable object
     */
    public DeployableObject getDeployableObject() {
        return null; // [todo] implement method
    }

    /**
     * Return the top level configuration for a deployment descriptor
     *
     * @param bean the root of the deployment descriptor
     * @return the configuration
     * @throws javax.enterprise.deploy.spi.exceptions.ConfigurationException for an error in the deployment descriptor
     */
    public DConfigBeanRoot getDConfigBeanRoot(DDBeanRoot bean) throws ConfigurationException {
        return null; // [todo] implement method
    }

    /**
     * Remove a root configuration and all its children
     *
     * @param bean the configuration
     * @throws javax.enterprise.deploy.spi.exceptions.BeanNotFoundException when the bean is not found
     */
    public void removeDConfigBean(DConfigBeanRoot bean) throws BeanNotFoundException {
        // [todo] implement method
    }

    /**
     * Restore a configuration from an input stream
     *
     * @param input the input stream
     * @param bean the deployment descriptor
     * @return the configuration
     * @throws javax.enterprise.deploy.spi.exceptions.ConfigurationException when there is an error in the configuration
     */
    public DConfigBeanRoot restoreDConfigBean(InputStream input, DDBeanRoot bean) throws ConfigurationException {
        return null; // [todo] implement method
    }

    /**
     * Save a configuration to an output stream
     *
     * @param output the output stream
     * @param bean the configuration
     * @throws javax.enterprise.deploy.spi.exceptions.ConfigurationException when there is an error in the configuration
     */
    public void saveDConfigBean(OutputStream output, DConfigBeanRoot bean) throws ConfigurationException {
        // [todo] implement method
    }

    /**
     * Restores a full set of configuration beans
     *
     * @param input the input stream
     * @throws javax.enterprise.deploy.spi.exceptions.ConfigurationException for an error in the configuration
     */
    public void restore(InputStream input) throws ConfigurationException {
        // [todo] implement method
    }

    /**
     * Saves the fulls set of configuration beans
     *
     * @param output the output stream
     * @throws javax.enterprise.deploy.spi.exceptions.ConfigurationException for an error in the configuration
     */
    public void save(OutputStream output) throws ConfigurationException {
        // [todo] implement method
    }
}
