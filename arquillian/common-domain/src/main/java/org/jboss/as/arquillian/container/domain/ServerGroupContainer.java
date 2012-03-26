/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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
package org.jboss.as.arquillian.container.domain;

import java.util.Iterator;
import java.util.Set;

import org.jboss.arquillian.container.spi.Container.State;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.as.arquillian.container.domain.Domain.Server;
import org.jboss.as.arquillian.container.domain.Domain.ServerGroup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.util.NotImplementedException;

/**
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class ServerGroupContainer implements DeployableContainer<EmptyConfiguration> {

    @Inject @ContainerScoped
    private InstanceProducer<ArchiveDeployer> archiveDeployerInst;

    @Inject @ContainerScoped
    private InstanceProducer<ModelControllerClient> clientInst;

    @Inject
    private Instance<ContainerRegistry> containerRegistryInst;

    private ManagementClient client;
    private ArchiveDeployer deployer;
    private ServerGroup serverGroup;
    private Domain domain;
    private int operationTimeout;

    public ServerGroupContainer(ManagementClient client, ArchiveDeployer deployer, Domain domain, ServerGroup serverGroup, int operationTimeout) {
        this.client = client;
        this.deployer = deployer;
        this.domain = domain;
        this.serverGroup = serverGroup;
        this.operationTimeout = operationTimeout;
    }

    @Override
    public Class<EmptyConfiguration> getConfigurationClass() {
        return EmptyConfiguration.class;
    }

    @Override
    public void setup(EmptyConfiguration configuration) {
        archiveDeployerInst.set(deployer);
        clientInst.set(client.getControllerClient());
    }

    @Override
    public void start() throws LifecycleException {
        client.startServerGroup(serverGroup.getName());

        waitForGroupMembers(true);
        updateGroupMembersContainerState(State.STARTED);
    }

    @Override
    public void stop() throws LifecycleException {
        client.stopServerGroup(serverGroup.getName());

        waitForGroupMembers(false);
        updateGroupMembersContainerState(State.STOPPED);
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        String uniqueName = deployer.deploy(archive, serverGroup.getName());

        ProtocolMetaData metaData = new ProtocolMetaData();
        for (Server server : domain.getServersInGroup(serverGroup)) {
            metaData.addContext(new LazyHttpContext(server, uniqueName, client));
        }
        return metaData;
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        deployer.undeploy(archive.getName(), serverGroup.getName());
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new NotImplementedException();
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new NotImplementedException();
    }

    private void waitForGroupMembers(boolean shouldBeStarted) {
        Set<Server> servers = domain.getServersInGroup(serverGroup);

        long timeout = operationTimeout * 1000;
        long sleep = 100;

        while (timeout > 0 && servers.size() > 0) {
            Iterator<Server> serverIterator = servers.iterator();
            while (serverIterator.hasNext()) {
                Server server = serverIterator.next();
                if (shouldBeStarted == client.isServerStarted(server)) {
                    serverIterator.remove();
                }
            }
            try {
                Thread.sleep(sleep);
                timeout -= sleep;
            } catch (InterruptedException e) {
                throw new RuntimeException("Failed waiting for servers to " + (shouldBeStarted ? "start" : "stop"), e);
            }
        }
        if(timeout <= 0) {
            throw new RuntimeException(
                    "Servers in group did not " + (shouldBeStarted ? "start":"stop") +
                    " within set timeout [serverGroupOperationTimeoutInSeconds=" + operationTimeout + "]. " + servers);
        }
    }

    /**
     * Update all Arquillian Containers in the Group with the new State.
     *
     * The Group can start/stop nodes outside of Arquillian's control.
     */
    private void updateGroupMembersContainerState(State newState) {
        Set<Server> servers = domain.getServersInGroup(serverGroup);

        ContainerRegistry registry = containerRegistryInst.get();

        for (Server server : servers) {
            registry.getContainer(server.getContainerName()).setState(newState);
        }
    }
}
