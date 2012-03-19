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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.jar.JarOutputStream;

import javax.enterprise.deploy.model.DDBean;
import javax.enterprise.deploy.model.DDBeanRoot;
import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.spi.DConfigBean;
import javax.enterprise.deploy.spi.DConfigBeanRoot;
import javax.enterprise.deploy.spi.exceptions.ConfigurationException;

import org.jboss.as.jsr88.spi.DeploymentMetaData;
import org.jboss.as.jsr88.spi.JarUtils;

/**
 * This class is a jboss-web config bean with only one required xpaths for its deployment descriptor.
 *
 * The required xpath for this descriptor are: jboss-web/context-root
 *
 * @author Rob Stryker
 *
 */
public class WarConfigBeanRoot extends JBossConfigBeanProxy implements DConfigBeanRoot {

    public static final String jbossWebXml = "jboss-web";
    public static final String jbossWebLocation = "!/WEB-INF/jboss-web.xml";
    public static final String jbossWebLocationTrimmed = "jboss-web.xml";
    public static final String deployPlanElement = "deployPlan";
    public static final String contextRoot = "context-root";
    public static final String archiveName = "archive-name";

    public WarConfigBeanRoot(DDBeanRoot standard, DeployableObject deployable) {
        WarConfigBean bean = new WarConfigBean(standard, this, null);
        setBean(bean, deployable);
    }

    public DConfigBean getDConfigBean(DDBeanRoot arg0) {
        /*
         * Get the filename for this bean root and send along a configbean for that type.
         *
         * This class assumes only one jboss-specific descriptor, so this method returns null.
         */
        return null;
    }

    private class WarConfigBean extends AbstractJBossConfigBean {
        public WarConfigBean(DDBean bean, DConfigBeanRoot root, ConfigBeanXPaths path) {
            super(bean, root, path);
        }

        protected ConfigBeanXPaths buildXPathList() {
            ConfigBeanXPaths pathRoot = new ConfigBeanXPaths("", null);

            // The constructor automatically sets parent and adds to child list of parent.
            new ConfigBeanXPaths(jbossWebXml, pathRoot);
            new ConfigBeanXPaths(deployPlanElement, pathRoot);
            return pathRoot;
        }

        public DConfigBean getDConfigBean(DDBean bean) throws ConfigurationException {
            // get a child bean
            String path = bean.getXpath();
            ConfigBeanXPaths cPath = (ConfigBeanXPaths) xpaths.get(path);
            if (cPath == null) {
                throw new ConfigurationException("Config Bean Not Found");
            }

            AbstractJBossConfigBean retBean = new JbossWebConfigBean(bean, this.myRoot, cPath);
            children.add(retBean);
            return retBean;
        }

        public void save(JarOutputStream jos, DeploymentMetaData metaData) {
            System.out.println("saving WarConfigBean");
            DDBean[] jbossWeb = myBean.getChildBean(jbossWebXml);
            DDBean[] deploymentPlan = myBean.getChildBean(deployPlanElement);
            if (jbossWeb.length == 0)
                return;
            if (deploymentPlan.length == 0)
                return;

            DDBean plan = deploymentPlan[0];
            String[] planNames = plan.getText(archiveName);
            if (planNames.length == 0)
                return;
            String warFileName = planNames[0];

            String webXml = jbossWeb[0].getText();
            System.out.println("name: " + warFileName);
            metaData.setDeploymentName(warFileName);
            InputStream stream = new ByteArrayInputStream(webXml.getBytes());
            try {
                JarUtils.addJarEntry(jos, jbossWebLocation, stream);
                metaData.addEntry(metaData.getDeploymentName(), jbossWebLocationTrimmed);
            } catch (Exception e) {
                System.out.println("ERROR HERE in SAVE: " + e.getMessage());
            }
        }

    }

    public class JbossWebConfigBean extends AbstractJBossConfigBean {
        private String stringPath;

        public JbossWebConfigBean(DDBean bean, DConfigBeanRoot root, ConfigBeanXPaths cPath) {
            super(bean, root, cPath);
        }

        protected ConfigBeanXPaths buildXPathList() {
            ConfigBeanXPaths pathRoot = new ConfigBeanXPaths("", null);
            if (this.myPath.getPath().equals(jbossWebXml))
                new ConfigBeanXPaths(contextRoot, pathRoot);
            else if (this.myPath.getPath().equals(deployPlanElement))
                new ConfigBeanXPaths(archiveName, pathRoot);
            return pathRoot;
        }

    }

}
