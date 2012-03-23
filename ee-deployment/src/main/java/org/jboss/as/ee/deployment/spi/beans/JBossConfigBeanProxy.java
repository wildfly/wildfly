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
package org.jboss.as.ee.deployment.spi.beans;

import java.beans.PropertyChangeListener;
import java.util.jar.JarOutputStream;

import javax.enterprise.deploy.model.DDBean;
import javax.enterprise.deploy.model.DDBeanRoot;
import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.model.XpathEvent;
import javax.enterprise.deploy.spi.DConfigBean;
import javax.enterprise.deploy.spi.DConfigBeanRoot;
import javax.enterprise.deploy.spi.exceptions.BeanNotFoundException;
import javax.enterprise.deploy.spi.exceptions.ConfigurationException;

import org.jboss.as.ee.deployment.spi.DeploymentMetaData;

/**
 * This class serves entirely as a proxy for ConfigBeanRoot types, which create an actual bean of a specified type.
 *
 * It's meant only to keep extending classes cleaner and smaller.
 *
 * @author Rob Stryker
 *
 */
public abstract class JBossConfigBeanProxy implements DConfigBeanRoot {

    protected AbstractJBossConfigBean myBean;
    protected DeployableObject myDeployable;

    protected void setBean(AbstractJBossConfigBean bean, DeployableObject deployable) {
        myBean = bean;
        myDeployable = deployable;
    }

    /**
     * This is the only abstract method.
     */
    public abstract DConfigBean getDConfigBean(DDBeanRoot arg0);

    public DDBean getDDBean() {
        return myBean.getDDBean();
    }

    public String[] getXpaths() {
        return myBean.getXpaths();
    }

    public DConfigBean getDConfigBean(DDBean bean) throws ConfigurationException {
        return myBean.getDConfigBean(bean);
    }

    public void removeDConfigBean(DConfigBean arg0) throws BeanNotFoundException {
        myBean.removeDConfigBean(arg0);
    }

    public void notifyDDChange(XpathEvent arg0) {
        myBean.notifyDDChange(arg0);
    }

    public void addPropertyChangeListener(PropertyChangeListener arg0) {
        myBean.addPropertyChangeListener(arg0);
    }

    public void removePropertyChangeListener(PropertyChangeListener arg0) {
        myBean.removePropertyChangeListener(arg0);
    }

    public void save(JarOutputStream jos, DeploymentMetaData metaData) {
        myBean.save(jos, metaData);
    }

}
