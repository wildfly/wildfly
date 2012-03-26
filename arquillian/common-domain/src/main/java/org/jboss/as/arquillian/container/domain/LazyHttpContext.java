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

import java.util.List;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.as.arquillian.container.domain.Domain.Server;

/**
 * We lookup deployment context lazy because the server in the server-group might not be started during deploy time.
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class LazyHttpContext extends HTTPContext {

    private Server server;
    private String deploymentName;
    private ManagementClient client;

    private HTTPContext context = null;

    public LazyHttpContext(Server server, String deploymentName, ManagementClient client) {
        super("localhost", -1);

        this.server = server;
        this.deploymentName = deploymentName;
        this.client = client;
    }

    @Override
    public String getName() {
        return server.getContainerName();
    }

    @Override
    public String getHost() {
        initiateContext();
        return context.getHost();
    }

    @Override
    public int getPort() {
        initiateContext();
        return context.getPort();
    }

    @Override
    public HTTPContext add(Servlet servlet) {
        initiateContext();
        return context.add(servlet);
    }

    @Override
    public List<Servlet> getServlets() {
        initiateContext();
        return context.getServlets();
    }

    @Override
    public Servlet getServletByName(String name) {
        initiateContext();
        return context.getServletByName(name);
    }

    private void initiateContext() {
        if (context == null) {
            context = client.getHTTPDeploymentMetaData(server, deploymentName);
        }
    }
}
