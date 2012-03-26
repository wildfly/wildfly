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

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.as.arquillian.container.domain.Domain.Server;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class ServerContainer implements DeployableContainer<EmptyConfiguration> {

    private ManagementClient client;
    private Server server;
    private int operationTimeout;

    public ServerContainer(ManagementClient client, Server server, int operationTimeout) {
        this.client = client;
        this.server = server;
        this.operationTimeout = operationTimeout;
    }

    @Override
    public Class<EmptyConfiguration> getConfigurationClass() {
        return EmptyConfiguration.class;
    }

    @Override
    public void setup(EmptyConfiguration configuration) {
    }

    @Override
    public void start() throws LifecycleException {
        client.startServer(server);

        waitForServerToStart();
    }

    @Override
    public void stop() throws LifecycleException {
        client.stopServer(server);

        waitForServerToStop();
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        throw new UnsupportedOperationException("Can not deploy to a single server in the domain, target server-group " + server.getGroup());
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        throw new UnsupportedOperationException("Can not undeploy from a single server in the domain, target server-group " + server.getGroup());
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Can not deploy to a single server in the domain, target server-group " + server.getGroup());
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Can not undeploy from a single server in the domain, target server-group " + server.getGroup());
    }

    private void waitForServerToStart() {
        waitForServerState(true);
    }

    private void waitForServerToStop() {
        waitForServerState(false);
    }

    private void waitForServerState(boolean shouldBeStarted) {
        long timeout = operationTimeout * 1000;
        long sleep = 100;

        while (timeout > 0) {
            if (shouldBeStarted == client.isServerStarted(server)) {
                break;
            }
            try {
                Thread.sleep(sleep);
                timeout -= sleep;
            } catch (InterruptedException e) {
                throw new RuntimeException("Failed waiting for server to " + (shouldBeStarted ? "start" : "stop"), e);
            }
        }
        if(timeout <= 0) {
            throw new RuntimeException(
                    "Server did not " + (shouldBeStarted ? "start":"stop") +
                    " within set timeout [serverOperationTimeoutInSeconds=" + operationTimeout + "]. " + server);
        }
    }
}
