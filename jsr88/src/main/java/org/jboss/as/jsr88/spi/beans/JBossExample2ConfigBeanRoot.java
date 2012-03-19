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
package org.jboss.as.jsr88.spi.beans;

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

public class JBossExample2ConfigBeanRoot extends JBossConfigBeanProxy implements DConfigBeanRoot {

    public JBossExample2ConfigBeanRoot(DDBeanRoot root) {
        JBossExample2MainConfigBean bean = new JBossExample2MainConfigBean(root, this, null);
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

    private class JBossExample2MainConfigBean extends AbstractJBossConfigBean {
        public JBossExample2MainConfigBean(DDBean bean, DConfigBeanRoot root, ConfigBeanXPaths path) {
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

            // ConfigBeanXPaths contextRoot =
            new ConfigBeanXPaths("root-element/sub-element", pathRoot);
            new ConfigBeanXPaths("root-element/other-sub", pathRoot);
            return pathRoot;
        }

        /**
         * Overloading the super-implementation so that we can return subtypes, such as JBossExample2SubElementConfigBean or
         * JBossExample2OtherSubConfigBean
         */
        public DConfigBean getDConfigBean(DDBean bean) throws ConfigurationException {
            // get a child bean
            String path = bean.getXpath();
            ConfigBeanXPaths cPath = (ConfigBeanXPaths) xpaths.get(path);
            if (cPath == null) {
                throw new ConfigurationException("Config Bean Not Found");
            }

            AbstractJBossConfigBean retBean;
            if (bean.getXpath().equals("root-element/sub-element")) {
                retBean = new JBossExample2SubElementConfigBean(bean, this.myRoot, cPath);
            } else if (bean.getXpath().equals("root-element/other-sub")) {
                retBean = new JBossExample2OtherSubConfigBean(bean, this.myRoot, cPath);
            } else {
                retBean = new JBossNullConfigBean(bean, this.myRoot, cPath);
            }

            children.add(retBean);
            return retBean;
        }
    }

    public class JBossExample2SubElementConfigBean extends AbstractJBossConfigBean {
        public JBossExample2SubElementConfigBean(DDBean bean, DConfigBeanRoot root, ConfigBeanXPaths path) {
            super(bean, root, path);
        }

        /**
         * The required xpaths for this example class are: root-element/sub-element[@id] root-element/sub-element/name
         * root-element/sub-element/class
         *
         * However this class is a sub-element type, so we only need
         *
         * @id name class
         */

        protected ConfigBeanXPaths buildXPathList() {
            ConfigBeanXPaths pathRoot = new ConfigBeanXPaths("", null);
            new ConfigBeanXPaths("@id", pathRoot);
            new ConfigBeanXPaths("name", pathRoot);
            new ConfigBeanXPaths("class", pathRoot);
            return pathRoot;
        }

    }

    public class JBossExample2OtherSubConfigBean extends AbstractJBossConfigBean {
        public JBossExample2OtherSubConfigBean(DDBean bean, DConfigBeanRoot root, ConfigBeanXPaths path) {
            super(bean, root, path);
        }

        /**
         * The required xpaths for this example class are: root-element/other-sub/name root-element/other-sub/description
         *
         * However this class is a sub-element type, so we only need name description
         */
        protected ConfigBeanXPaths buildXPathList() {
            ConfigBeanXPaths pathRoot = new ConfigBeanXPaths("", null);
            new ConfigBeanXPaths("name", pathRoot);
            new ConfigBeanXPaths("description", pathRoot);
            return pathRoot;
        }
    }
}
