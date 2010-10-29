/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.standalone.client.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Future;

import org.jboss.as.standalone.client.api.deployment.DeploymentPlan;
import org.jboss.as.standalone.client.api.deployment.DuplicateDeploymentNameException;
import org.jboss.as.standalone.client.api.deployment.InitialDeploymentPlanBuilder;
import org.jboss.as.standalone.client.api.deployment.ServerDeploymentManager;
import org.jboss.as.standalone.client.api.deployment.ServerDeploymentPlanResult;
import org.jboss.as.standalone.client.impl.deployment.DeploymentContentDistributor;
import org.jboss.as.standalone.client.impl.deployment.InitialDeploymentPlanBuilderFactory;

/**
 * @author Emanuel Muckenhuber
 */
class StandaloneClientDeploymentManager implements ServerDeploymentManager {

    private final StandaloneClientImpl client;
    private final DeploymentContentDistributor contentDistributor;


    StandaloneClientDeploymentManager(StandaloneClientImpl client) {
        assert client != null : "client is null";
        this.client = client;

        this.contentDistributor = new DeploymentContentDistributor() {
            @Override
            public byte[] distributeDeploymentContent(String name, String runtimeName, InputStream stream)
                    throws IOException, DuplicateDeploymentNameException {
                boolean unique = StandaloneClientDeploymentManager.this.client.isDeploymentNameUnique(name);
                if (!unique) {
                    throw new DuplicateDeploymentNameException(name, false);
                }
                return StandaloneClientDeploymentManager.this.client.addDeploymentContent(name, runtimeName, stream);
            }
            @Override
            public byte[] distributeReplacementDeploymentContent(String name, String runtimeName, InputStream stream)
                    throws IOException {
                return StandaloneClientDeploymentManager.this.client.addDeploymentContent(name, runtimeName, stream);
            }
        };
    }

    @Override
    public String addDeploymentContent(File file) throws IOException, DuplicateDeploymentNameException {
        String name = file.getName();
        getDeploymentContentDistributor().distributeDeploymentContent(name, name, new FileInputStream(file));
        return name;
    }

    @Override
    public String addDeploymentContent(URL url) throws IOException, DuplicateDeploymentNameException {
        String name = getName(url);
        addDeploymentContent(name, name, url);
        return name;
    }

    @Override
    public void addDeploymentContent(String name, File file) throws IOException, DuplicateDeploymentNameException {
        String commonName = file.getName();
        getDeploymentContentDistributor().distributeDeploymentContent(name, commonName, new FileInputStream(file));
    }

    @Override
    public void addDeploymentContent(String name, URL url) throws IOException, DuplicateDeploymentNameException {
        String commonName = getName(url);
        addDeploymentContent(name, commonName , url);
    }

    private void addDeploymentContent(String name, String commonName, URL url) throws IOException, DuplicateDeploymentNameException {
        URLConnection conn = url.openConnection();
        conn.connect();
        getDeploymentContentDistributor().distributeDeploymentContent(name, commonName, conn.getInputStream());
    }

    @Override
    public void addDeploymentContent(String name, InputStream stream) throws IOException, DuplicateDeploymentNameException {
        addDeploymentContent(name, name, stream);
    }

    @Override
    public void addDeploymentContent(String name, String commonName, InputStream stream) throws IOException, DuplicateDeploymentNameException {
        getDeploymentContentDistributor().distributeDeploymentContent(name, commonName, stream);
    }

    /** {@inheritDoc} */
    public InitialDeploymentPlanBuilder newDeploymentPlan() {
        return InitialDeploymentPlanBuilderFactory.newInitialDeploymentPlanBuilder(contentDistributor);
    }

    /** {@inheritDoc} */
    public Future<ServerDeploymentPlanResult> execute(DeploymentPlan plan) {
        return this.client.execute(plan);
    }

    /**
     * @return the contentDistributor
     */
    DeploymentContentDistributor getDeploymentContentDistributor() {
        return contentDistributor;
    }

    private static String getName(URL url) {
        if ("file".equals(url.getProtocol())) {
            try {
                File f = new File(url.toURI());
                return f.getName();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(url + " is not a valid URI", e);
            }
        }

        String path = url.getPath();
        int idx = path.lastIndexOf('/');
        while (idx == path.length() - 1) {
            path = path.substring(0, idx);
            idx = path.lastIndexOf('/');
        }
        if (idx == -1) {
            throw new IllegalArgumentException("Cannot derive a deployment name from " +
                    url + " -- use an overloaded method variant that takes a 'name' parameter");
        }

        return path.substring(idx + 1);
    }
}
