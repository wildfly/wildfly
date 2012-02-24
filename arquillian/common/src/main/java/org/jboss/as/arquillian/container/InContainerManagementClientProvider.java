/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

import java.lang.annotation.Annotation;

import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.modules.ModuleClassLoader;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;

/**
 *
 */
public class InContainerManagementClientProvider implements ResourceProvider {


    private static ManagementClient current;

    @Inject
    private Instance<ContainerConfiguration> configuration;

    /**
     * {@inheritDoc}
     * @see org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider#canProvide(Class)
     */
    @Override
    public boolean canProvide(final Class<?> type) {
        return type.isAssignableFrom(ManagementClient.class);
    }

    @Override
    public synchronized Object lookup(final ArquillianResource arquillianResource, final Annotation... annotations) {
        if(current != null ) {
            return current;
        }
        //hack to figure out if we are running in container
        if(getClass().getClassLoader() instanceof ModuleClassLoader) {
            ContainerConfiguration config = configuration.get();
            if(config instanceof CommonContainerConfiguration) {
                final CommonContainerConfiguration containerConfig = (CommonContainerConfiguration)config;
                ModelControllerClient modelControllerClient = ModelControllerClient.Factory.create(
                        containerConfig.getManagementAddress(),
                        containerConfig.getManagementPort(),
                        getCallbackHandler());
                current = new ManagementClient(modelControllerClient, containerConfig.getManagementAddress().getHostAddress(), containerConfig.getManagementPort());
            }
        }
        return current;
    }

    public synchronized void cleanUp(@Observes AfterSuite afterSuite) {
        current.close();
        current = null;
    }
}
