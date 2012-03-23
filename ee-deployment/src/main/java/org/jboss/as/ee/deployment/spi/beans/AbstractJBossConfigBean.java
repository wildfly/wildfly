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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.JarOutputStream;

import javax.enterprise.deploy.model.DDBean;
import javax.enterprise.deploy.model.XpathEvent;
import javax.enterprise.deploy.spi.DConfigBean;
import javax.enterprise.deploy.spi.DConfigBeanRoot;
import javax.enterprise.deploy.spi.exceptions.BeanNotFoundException;
import javax.enterprise.deploy.spi.exceptions.ConfigurationException;

import org.jboss.as.ee.deployment.spi.DeploymentMetaData;

/**
 * @author Rob Stryker
 *
 */
public abstract class AbstractJBossConfigBean implements DConfigBean {

    protected DDBean myBean;
    protected ArrayList myPropertyListeners;
    protected HashMap xpaths;
    protected DConfigBeanRoot myRoot;
    protected ConfigBeanXPaths myPath;
    protected ArrayList children;

    public AbstractJBossConfigBean(DDBean bean, DConfigBeanRoot root, ConfigBeanXPaths path) {
        myBean = bean;
        myRoot = root;
        myPropertyListeners = new ArrayList();
        xpaths = new HashMap();
        myPath = path;
        children = new ArrayList();

        /*
         * fill our map this map is to more easily parse through xpaths via map.get(string) rather than iterate through an
         * arraylist until name matches
         */

        ConfigBeanXPaths xpathList = buildXPathList();
        Iterator i = xpathList.getChildren().iterator();
        while (i.hasNext()) {
            ConfigBeanXPaths x = (ConfigBeanXPaths) i.next();
            xpaths.put(x.getPath(), x);
        }

    }

    public DDBean getDDBean() {
        return myBean;
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        myPropertyListeners.add(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        myPropertyListeners.remove(pcl);
    }

    /**
     * Removes the xpath of a given config bean from the list of this bean's xpaths (children so to speak). Then calls death.
     */
    public void removeDConfigBean(DConfigBean bean) throws BeanNotFoundException {
        // childList.remove(bean);
        AbstractJBossConfigBean b = ((AbstractJBossConfigBean) bean);
        Object o = xpaths.get(b.getPath());

        if (o == null) {
            throw new BeanNotFoundException(b.getPath());
        }
        children.remove(bean);
        xpaths.remove(b.getPath());
        b.death();
    }

    public String[] getXpaths() {

        Object[] paths = this.xpaths.values().toArray();
        String[] retval = new String[paths.length];
        for (int i = 0; i < paths.length; i++) {
            retval[i] = ((ConfigBeanXPaths) paths[i]).getPath();
        }
        return retval;

    }

    public DConfigBean getDConfigBean(DDBean bean) throws ConfigurationException {
        // get a child bean
        String path = bean.getXpath();
        ConfigBeanXPaths cPath = (ConfigBeanXPaths) xpaths.get(path);
        if (cPath == null) {
            return null;
        }

        AbstractJBossConfigBean retBean = new JBossNullConfigBean(bean, this.myRoot, cPath);
        children.add(retBean);
        return retBean;
    }

    public class JBossNullConfigBean extends AbstractJBossConfigBean {
        public JBossNullConfigBean(DDBean bean, DConfigBeanRoot root, ConfigBeanXPaths path) {
            super(bean, root, path);
        }

        /**
         * This bean requires no other xpaths.
         */
        protected ConfigBeanXPaths buildXPathList() {
            ConfigBeanXPaths pathRoot = new ConfigBeanXPaths("", null);
            return pathRoot;
        }

        /**
         * All children attempts will return null.
         */
        public DConfigBean getDConfigBean(DDBean bean) throws ConfigurationException {
            throw new ConfigurationException(bean.getXpath());
        }
    }

    public String getPath() {
        return myPath.getPath();
    }

    /*
     * Deletes the children, and also tells them to delete their children.
     */
    protected void death() {
        Iterator i = children.iterator();
        while (i.hasNext()) {
            AbstractJBossConfigBean b = (AbstractJBossConfigBean) i.next();
            try {
                removeDConfigBean(b);
            } catch (BeanNotFoundException e) {
            }
        }
        xpaths.clear();
    }

    public void notifyDDChange(XpathEvent arg0) {

    }

    // OVER RIDE ME
    public void save(JarOutputStream stream, DeploymentMetaData metaData) {

    }

    protected abstract ConfigBeanXPaths buildXPathList();

}
