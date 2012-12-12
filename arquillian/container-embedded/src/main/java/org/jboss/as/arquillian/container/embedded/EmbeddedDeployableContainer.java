/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.arquillian.container.embedded;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.as.arquillian.container.CommonDeployableContainer;
import org.jboss.as.embedded.EmbeddedServerFactory;
import org.jboss.as.embedded.StandaloneServer;

/**
 * {@link DeployableContainer} implementation to bootstrap JBoss Logging (installing the LogManager if possible), use the JBoss
 * Modules modular ClassLoading Environment to create a new server instance, and handle lifecycle of the Application Server in
 * the currently-running environment.
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 * @author <a href="mailto:mmatloka@gmail.com">Michal Matloka</a>
 * @author Thomas.Diesler@jboss.com
 */
public final class EmbeddedDeployableContainer extends CommonDeployableContainer<EmbeddedContainerConfiguration> {

    /**
     * Hook to the server; used in start/stop, created by setup
     */
    private StandaloneServer server;

    @Override
    public void setup(final EmbeddedContainerConfiguration config) {
        super.setup(config);
        server = EmbeddedServerFactory.create(config.getJbossHome(), config.getModulePath(), config.getBundlePath());
    }

    @Override
    public Class<EmbeddedContainerConfiguration> getConfigurationClass() {
        return EmbeddedContainerConfiguration.class;
    }

    @Override
    protected void startInternal() throws LifecycleException {
        try {
            server.start();
        } catch (Throwable e) {
            throw new LifecycleException("Could not invoke start on: " + server, e);
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        try {
            server.stop();
        } catch (Throwable e) {
            throw new LifecycleException("Could not invoke stop on: " + server, e);
        }
    }
}
