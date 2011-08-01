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
package org.jboss.deployment.services;

/**
 * An mbean service interface for the server side functionality needed for the DeploymentManager implementation.
 *
 * [TODO remove]
 *
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 *
 */
public interface DeploymentManagerServiceMBean {
    /*
     * public Controller getController(); public void setController(Controller controller); public File getUploadDir();
     *
     * public void setUploadDir(File uploadDir);
     *
     * public MainDeployerImpl getMainDeployer(); public void setMainDeployer(MainDeployerImpl deployer);
     *
     * public Class getCarDeployerType(); public void setCarDeployerType(Class carDeployerType); public Class
     * getEarDeployerType(); public void setEarDeployerType(Class earDeployerType); public Class getEjbDeployerType(); public
     * void setEjbDeployerType(Class ejbDeployerType); public Class getEjb3DeployerType(); public void setEjb3DeployerType(Class
     * ejb3DeployerType); public Class getRarDeployerType(); public void setRarDeployerType(Class rarDeployerType); public Class
     * getWarDeployerType(); public void setWarDeployerType(Class warDeployerType);
     *
     * boolean isDeleteOnUndeploy();
     *
     * void setDeleteOnUndeploy(boolean deleteOnUndeploy);
     *
     * boolean isFailOnCollision();
     *
     * void setFailOnCollision(boolean failOnCollision);
     *
     * public Map getModuleMap();
     *
     * public void deploy(SerializableTargetModuleID moduleID) throws Exception;
     *
     * public void start(String url) throws Exception;
     *
     * public void stop(String url) throws Exception;
     *
     * public void undeploy(String url) throws Exception;
     *
     * SerializableTargetModuleID[] getAvailableModules(int moduleType) throws TargetException;
     */
}
