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

import javax.enterprise.deploy.model.DDBean;
import javax.enterprise.deploy.model.DDBeanRoot;
import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.spi.DConfigBean;
import javax.enterprise.deploy.spi.DConfigBeanRoot;
import javax.enterprise.deploy.spi.exceptions.ConfigurationException;

/**
 * This class is an example of how to build config beans based on a number of required xpaths for a deployment descriptor.
 *
 * The required xpaths for this example class are: root-element/sub-element[@id] root-element/sub-element/name
 * root-element/sub-element/class
 *
 * root-element/other-sub/name root-element/other-sub/description
 *
 * This example class will return the xpaths as ONE list. Any attempt to get a child bean will return a null-bean, as the
 * children beans will not require additional xpaths.
 *
 * @author Rob Stryker
 *
 *
 *         TODO To change the template for this generated type comment go to Window - Preferences - Java - Code Style - Code
 *         Templates
 */

public class JBossExample1ConfigBeanRoot extends JBossConfigBeanProxy implements DConfigBeanRoot {

    public JBossExample1ConfigBeanRoot(DDBeanRoot root) {
        JBossExample1MainConfigBean bean = new JBossExample1MainConfigBean(root, this, null);
        DeployableObject deployment = root.getDeployableObject();
        setBean(bean, deployment);
    }

    public DConfigBean getDConfigBean(DDBeanRoot arg0) {
        /*
         * Get the filename for this bean root and send along a configbean for that type.
         *
         * This example assumes only one jboss-specific descriptor, so this method returns null.
         */
        return null;
    }

    private class JBossExample1MainConfigBean extends AbstractJBossConfigBean {
        public JBossExample1MainConfigBean(DDBean bean, DConfigBeanRoot root, ConfigBeanXPaths path) {
            super(bean, root, path);
        }

        /**
         * In this example, every required xpath will be returned in ONE list.
         *
         * The required xpaths for this example class are: root-element/sub-element[@id] root-element/sub-element/name
         * root-element/sub-element/class
         *
         * root-element/other-sub/name root-element/other-sub/description
         */
        protected ConfigBeanXPaths buildXPathList() {
            ConfigBeanXPaths pathRoot = new ConfigBeanXPaths("", null);

            // The constructor automatically sets parent and adds to child list of parent.
            new ConfigBeanXPaths("root-element/sub-element[@id]", pathRoot);
            new ConfigBeanXPaths("root-element/sub-element/name", pathRoot);
            new ConfigBeanXPaths("root-element/sub-element/class", pathRoot);
            new ConfigBeanXPaths("root-element/other-sub/name", pathRoot);
            new ConfigBeanXPaths("root-element/other-sub/description", pathRoot);

            return pathRoot;
        }

        public DConfigBean getDConfigBean(DDBean bean) throws ConfigurationException {
            // get a child bean
            String path = bean.getXpath();
            ConfigBeanXPaths cPath = (ConfigBeanXPaths) xpaths.get(path);
            if (cPath == null) {
                throw new ConfigurationException("Config Bean Not Found");
            }

            AbstractJBossConfigBean retBean = new JBossNullConfigBean(bean, this.myRoot, cPath);
            children.add(retBean);
            return retBean;
        }
    }

}
