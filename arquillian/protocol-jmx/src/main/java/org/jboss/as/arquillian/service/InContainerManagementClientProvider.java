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
package org.jboss.as.arquillian.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.annotation.Annotation;
import java.net.URL;

import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;

/**
 *
 */
public class InContainerManagementClientProvider implements ResourceProvider {

    private static ManagementClient current;

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider#canProvide(Class)
     */
    @Override
    public boolean canProvide(final Class<?> type) {
        return type.isAssignableFrom(ManagementClient.class);
    }

    @Override
    public synchronized Object lookup(final ArquillianResource arquillianResource, final Annotation... annotations) {
        if (current != null) {
            return current;
        }
        final URL url = getClass().getClassLoader().getResource("META-INF/org.jboss.as.managementConnectionProps");
        if (url != null) {
            InputStream in = null;
            String managementPort;
            String address;
            try {
                in = url.openStream();
                ObjectInputStream inputStream = new ObjectInputStream(in);
                managementPort = (String)inputStream.readObject();
                address = (String) inputStream.readObject();
                if(address == null) {
                    address = "127.0.0.1";
                }
                if(managementPort == null) {
                    managementPort = "9999";
                }
                int realPort = Integer.parseInt(managementPort);
                ModelControllerClient modelControllerClient = ModelControllerClient.Factory.create(
                        address,
                        realPort,
                        getCallbackHandler());
                current = new ManagementClient(modelControllerClient, address, realPort);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }

        }
        return current;
    }

    public synchronized void cleanUp(@Observes AfterSuite afterSuite) {
        if (current != null) {
            current.close();
            current = null;
        }
    }
}
