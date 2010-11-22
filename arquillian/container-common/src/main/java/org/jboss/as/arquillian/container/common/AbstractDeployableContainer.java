/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.arquillian.spi.Configuration;
import org.jboss.arquillian.spi.ContainerMethodExecutor;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.DeployableContainer;
import org.jboss.arquillian.spi.DeploymentException;
import org.jboss.as.arquillian.jmx.JMXMethodExecutor;
import org.jboss.as.arquillian.jmx.JMXTestRunnerMBean;
import org.jboss.as.arquillian.jmx.JMXMethodExecutor.ExecutionType;
import org.jboss.as.standalone.client.api.StandaloneClient;
import org.jboss.as.standalone.client.api.deployment.DeploymentAction;
import org.jboss.as.standalone.client.api.deployment.DeploymentPlan;
import org.jboss.as.standalone.client.api.deployment.DeploymentPlanBuilder;
import org.jboss.as.standalone.client.api.deployment.ServerDeploymentActionResult;
import org.jboss.as.standalone.client.api.deployment.ServerDeploymentManager;
import org.jboss.as.standalone.client.api.deployment.ServerDeploymentPlanResult;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

/**
 * A JBossAS server connector
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public abstract class AbstractDeployableContainer implements DeployableContainer {
    private final Logger log = Logger.getLogger(AbstractDeployableContainer.class.getName());

    private JBossAsContainerConfiguration config;
    private ServerDeploymentManager deploymentManager;
    private JMXConnector jmxConnector;

    private final Map<Archive<?>, String> registry = new HashMap<Archive<?>, String>();

    @Override
    public void setup(Context context, Configuration configuration) {
        config = configuration.getContainerConfig(JBossAsContainerConfiguration.class);
        StandaloneClient client = StandaloneClient.Factory.create(config.getBindAddress(), config.getManagementPort());
        deploymentManager = client.getDeploymentManager();
    }

    public MBeanServerConnection getMBeanServerConnection() {
        int port = config.getJmxPort();
        String host = config.getBindAddress().getHostAddress();
        String urlString = System.getProperty("jmx.service.url", "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi");
        try {
            if (jmxConnector == null) {
                log.fine("Connecting JMXConnector to: " + urlString);
                JMXServiceURL serviceURL = new JMXServiceURL(urlString);
                jmxConnector = JMXConnectorFactory.connect(serviceURL, null);
            }

            return jmxConnector.getMBeanServerConnection();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot obtain MBeanServerConnection to: " + urlString, ex);
        }
    }

    @Override
    public ContainerMethodExecutor deploy(Context context, Archive<?> archive) throws DeploymentException {
        try {
            InputStream input = archive.as(ZipExporter.class).exportZip();
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            builder = builder.add(archive.getName(), input).andDeploy();
            DeploymentPlan plan = builder.build();
            DeploymentAction deployAction = builder.getLastAction();
            String runtimeName = executeDeploymentPlan(plan, deployAction);
            registry.put(archive, runtimeName);

            return getContainerMethodExecutor();
        } catch (Exception e) {
            throw new DeploymentException("Could not deploy to container", e);
        }
    }

    @Override
    public void undeploy(Context context, Archive<?> archive) throws DeploymentException {
        String runtimeName = registry.remove(archive);
        if (runtimeName != null) {
            try {
                DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
                DeploymentPlan plan = builder.undeploy(runtimeName).remove(runtimeName).build();
                Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
                future.get();
            } catch (Exception ex) {
                log.warning("Cannot undeploy: " + runtimeName + ":" + ex.getMessage());
            }
        }
    }

    private String executeDeploymentPlan(DeploymentPlan plan, DeploymentAction deployAction) throws Exception {
        Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        ServerDeploymentPlanResult planResult = future.get();

        ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(deployAction.getId());
        if (actionResult != null) {
            Exception deploymentException = (Exception) actionResult.getDeploymentException();
            if (deploymentException != null)
                throw deploymentException;
        }

        return deployAction.getDeploymentUnitUniqueName();
    }

    protected ContainerMethodExecutor getContainerMethodExecutor() {
         return new JMXMethodExecutor(getMBeanServerConnection(), ExecutionType.REMOTE, JMXTestRunnerMBean.OBJECT_NAME);
    }
}