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
package org.jboss.as.ee.deployment.spi.configurations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.JarOutputStream;

import javax.enterprise.deploy.model.DDBeanRoot;
import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.spi.DConfigBeanRoot;
import javax.enterprise.deploy.spi.DeploymentConfiguration;
import javax.enterprise.deploy.spi.exceptions.BeanNotFoundException;
import javax.enterprise.deploy.spi.exceptions.ConfigurationException;

import org.jboss.as.ee.deployment.spi.DeploymentLogger;
import org.jboss.as.ee.deployment.spi.DeploymentMetaData;
import org.jboss.as.ee.deployment.spi.JarUtils;
import org.jboss.as.ee.deployment.spi.beans.JBossConfigBeanProxy;
import org.jboss.as.ee.deployment.spi.beans.WarConfigBeanRoot;

/**
 * The war configuration container.
 *
 * @author Rob Stryker
 *
 */
public class WarConfiguration implements DeploymentConfiguration {

    private DeployableObject deployable;
    private HashMap configBeans;

    public WarConfiguration(DeployableObject deployable) {
        this.deployable = deployable;
        configBeans = new HashMap(); // maps filename to dconfigbean

    }

    public DeployableObject getDeployableObject() {
        return this.deployable;
    }

    public DConfigBeanRoot getDConfigBeanRoot(DDBeanRoot dd) throws ConfigurationException {
        // If they give us web.xml, return our jboss-web.xml config bean
        if (configBeans.containsKey(dd.getFilename())) {
            return (DConfigBeanRoot) configBeans.get(dd.getFilename());
        }

        // Not found, so create it. (lazy initializing)
        if (dd.getFilename().equals("WEB-INF/web.xml")) {
            DConfigBeanRoot retval = new WarConfigBeanRoot(dd, deployable);
            configBeans.put(dd.getFilename(), retval);
            return retval;
        }

        // if they give us some other standard bean, return the jboss specific
        // None implemented as of now
        return null;
    }

    public void removeDConfigBean(DConfigBeanRoot bean) throws BeanNotFoundException {
        String key = bean.getDDBean().getRoot().getFilename();
        if (configBeans.containsKey(key)) {
            configBeans.remove(key);
        } else {
            throw new BeanNotFoundException(bean.getDDBean().getXpath());
        }
    }

    public void save(OutputStream stream) throws ConfigurationException {
        JarOutputStream jos;

        // Setup deployment plan meta data with propriatary descriptor (jboss-web.xml)
        DeploymentMetaData metaData = new DeploymentMetaData("WRONG.war");

        try {
            jos = new JarOutputStream(stream);
        } catch (Exception e) {
            return;
        }
        if (jos == null)
            return;

        Iterator setIterator = configBeans.keySet().iterator();
        while (setIterator.hasNext()) {
            String key = (String) setIterator.next();
            JBossConfigBeanProxy val = (JBossConfigBeanProxy) configBeans.get(key);
            val.save(jos, metaData);
        }
        String metaStr = metaData.toXMLString();
        try {
            JarUtils.addJarEntry(jos, DeploymentMetaData.ENTRY_NAME, new ByteArrayInputStream(metaStr.getBytes()));
            jos.flush();
            jos.close();
        } catch (Exception e) {
            DeploymentLogger.ROOT_LOGGER.cannotSaveDeploymentPlanEntry(e, metaStr);
        }
    }

    public DConfigBeanRoot restoreDConfigBean(InputStream arg0, DDBeanRoot arg1) throws ConfigurationException {
        return null;
    }

    public void saveDConfigBean(OutputStream arg0, DConfigBeanRoot arg1) throws ConfigurationException {
    }

    public void restore(InputStream arg0) throws ConfigurationException {
    }

}
