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
 * A simple war dconfigbeanroot implementation
 *
 * @author Rob Stryker
 *
 */
public class JBossWebConfigBeanRoot extends JBossConfigBeanProxy implements DConfigBeanRoot {

    public JBossWebConfigBeanRoot(DDBeanRoot root) {
        JBossWebConfigBean bean = new JBossWebConfigBean(root, this);
        DeployableObject deployment = root.getDeployableObject();
        setBean(bean, deployment);
    }

    public DConfigBean getDConfigBean(DDBeanRoot arg0) {
        // get the filename for this bean and send along a configbean for that type.
        return null;
    }

    private class JBossWebConfigBean extends AbstractJBossConfigBean {
        public JBossWebConfigBean(DDBean bean, DConfigBeanRoot root) {
            super(bean, root, null);
        }

        public DConfigBean getDConfigBean(DDBean bean) throws ConfigurationException {
            // get a child bean
            return null;
        }

        /**
         * Called from the abstract super constructor
         */
        protected ConfigBeanXPaths buildXPathList() {
            ConfigBeanXPaths pathRoot = new ConfigBeanXPaths("", null);
            ConfigBeanXPaths contextRoot = new ConfigBeanXPaths("context-root", pathRoot);
            return pathRoot;
        }
    }

}
