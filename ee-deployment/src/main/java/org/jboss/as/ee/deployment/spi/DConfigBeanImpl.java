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

import java.beans.PropertyChangeListener;

import javax.enterprise.deploy.model.DDBean;
import javax.enterprise.deploy.model.XpathEvent;
import javax.enterprise.deploy.spi.DConfigBean;
import javax.enterprise.deploy.spi.exceptions.BeanNotFoundException;
import javax.enterprise.deploy.spi.exceptions.ConfigurationException;

/**
 * The DConfigBean is a deployment configuration bean (DConfigBean) that is associated with one or more deployment descriptor
 * beans, (DDBean). A DConfigBean represents a logical grouping of deployment configuration data to be presented to the
 * Deployer. A DConfigBean provides zero or more XPaths that identifies the XML information it requires. A DConfigBean may
 * contain other DConfigBeans and regular JavaBeans. The top most DConfigBean is a DConfigBeanRoot object which represents a
 * single XML instance document.
 *
 * @author Thomas.Diesler@jboss.com
 *
 */
class DConfigBeanImpl implements DConfigBean {
    /**
     * Get the XML text for this configuration
     *
     * @return the xml text
     */
    public DDBean getDDBean() {
        return null; // [todo] implement method
    }

    /**
     * Get the xpaths this deployment descriptor requires
     *
     * @return the xpaths
     */
    public String[] getXpaths() {
        return new String[0]; // [todo] implement method
    }

    /**
     * Return the JavaBean containing server specific deployment information
     *
     * @param bean the xml data to be evaluated
     * @return the server specific configuration
     * @throws javax.enterprise.deploy.spi.exceptions.ConfigurationException for errors generating the configuring bean
     */
    public DConfigBean getDConfigBean(DDBean bean) throws ConfigurationException {
        return null; // [todo] implement method
    }

    /**
     * Remove a child
     *
     * @param bean the child
     * @throws javax.enterprise.deploy.spi.exceptions.BeanNotFoundException when the bean is not found
     */
    public void removeDConfigBean(DConfigBean bean) throws BeanNotFoundException {
        // [todo] implement method
    }

    /**
     * A notification that the DDBean provided has changed and that this bean or child needs re-evaluating
     *
     * @param event the event
     */
    public void notifyDDChange(XpathEvent event) {
        // [todo] implement method
    }

    /**
     * Add a property change listener
     *
     * @param listener the listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // [todo] implement method
    }

    /**
     * Remove a property change listener
     *
     * @param listener the listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        // [todo] implement method
    }
}
