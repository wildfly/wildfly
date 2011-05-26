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
package org.jboss.as.arquillian.container;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.modules.management.ObjectProperties;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 * A JBossAS server connector
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public abstract class AbstractDeployableContainer<T extends JBossAsCommonConfiguration> implements DeployableContainer<T> {

    protected static final ObjectName OBJECT_NAME;

    static {
        try {
            OBJECT_NAME = new ObjectName("jboss.msc", ObjectProperties.properties(
                    ObjectProperties.property("type", "container"), ObjectProperties.property("name", "jboss-as")));
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Logger log = Logger.getLogger(AbstractDeployableContainer.class.getName());

    private T containerConfig;
    private ModelControllerClient modelControllerClient;
    private ServerDeploymentManager deploymentManager;

    private final Map<Object, String> registry = new HashMap<Object, String>();

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }

    @Override
    public void setup(T configuration) {
        containerConfig = configuration;
        modelControllerClient = ModelControllerClient.Factory.create(containerConfig.getBindAddress(),
                containerConfig.getManagementPort());

        deploymentManager = ServerDeploymentManager.Factory.create(modelControllerClient);
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        deploy(descriptor.getDescriptorName(), descriptor, new ByteArrayInputStream(descriptor.exportAsString().getBytes()));
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        undeploy((Object) descriptor);
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        String uniqueDeploymentName = deploy(archive.getName(), archive, archive.as(ZipExporter.class).exportAsInputStream());

        return getProtocolMetaData(archive, uniqueDeploymentName);
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        undeploy((Object) archive);
    }

    // TODO: can't be done in a proper way, hack until Management API support Deployment Metadata
    // protected abstract ProtocolMetaData getProtocolMetaData(String uniqueDeploymentName);

    protected ProtocolMetaData getProtocolMetaData(Archive<?> archive, String uniqueDeploymentName) {
        ProtocolMetaData protocol = new ProtocolMetaData().addContext(new HTTPContext(containerConfig.getBindAddress()
                .getHostAddress(), containerConfig.getHttpPort()).add(new Servlet("ArquillianServletRunner",
                getContextRootName(archive))));

        return protocol;
    }

    protected T getContainerConfiguration() {
        return containerConfig;
    }

    protected ModelControllerClient getModelControllerClient() {
        return modelControllerClient;
    }

    private String getContextRootName(Archive<?> archive) {
        String archiveName = archive.getName();
        if (archiveName.indexOf('.') != -1) {
            return archiveName.substring(0, archiveName.indexOf('.'));
        }
        return archiveName;
    }

    private void undeploy(Object deployment) throws DeploymentException {
        String runtimeName = registry.remove(deployment);
        if (runtimeName != null) {
            try {
                DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan().withRollback();
                DeploymentPlan plan = builder.undeploy(runtimeName).remove(runtimeName).build();
                Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
                future.get();
            } catch (Exception ex) {
                log.warning("Cannot undeploy: " + runtimeName + ":" + ex.getMessage());
            }
        }
    }

    private String deploy(String deploymentName, Object deployment, InputStream content) throws DeploymentException {
        try {
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan().withRollback();
            builder = builder.add(deploymentName, content).andDeploy();
            DeploymentPlan plan = builder.build();
            DeploymentAction deployAction = builder.getLastAction();

            return executeDeploymentPlan(plan, deployAction, deployment);

        } catch (Exception e) {
            throw new DeploymentException("Could not deploy to container", e);
        }
    }

    private String executeDeploymentPlan(DeploymentPlan plan, DeploymentAction deployAction, Object deployment)
            throws Exception {
        Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        registry.put(deployment, deployAction.getDeploymentUnitUniqueName());
        ServerDeploymentPlanResult planResult = future.get();

        ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(deployAction.getId());
        if (actionResult != null) {
            Exception deploymentException = (Exception) actionResult.getDeploymentException();
            if (deploymentException != null)
                throw deploymentException;
        }
        return deployAction.getDeploymentUnitUniqueName();
    }
}
