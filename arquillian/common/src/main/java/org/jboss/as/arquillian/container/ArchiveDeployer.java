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

import java.io.IOException;
import java.io.InputStream;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

/**
 * A deployer that uses the {@link ServerDeploymentHelper}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public class ArchiveDeployer {

    private static final Logger log = Logger.getLogger(ArchiveDeployer.class);

    private final ServerDeploymentHelper deployer;

    public ArchiveDeployer(ModelControllerClient modelControllerClient) {
        this.deployer = new ServerDeploymentHelper(modelControllerClient);
    }

    public String deploy(Archive<?> archive) throws DeploymentException {
        return deployInternal(archive);
    }

    public String deploy(String name, InputStream input) throws DeploymentException {
        return deployInternal(name, input);
    }

    public void undeploy(String runtimeName) throws DeploymentException {
        try {
            deployer.undeploy(runtimeName);
        } catch (Exception ex) {
            log.warnf(ex, "Cannot undeploy: %s", runtimeName);
        }
    }

    private String deployInternal(Archive<?> archive) throws DeploymentException {
        final InputStream input = archive.as(ZipExporter.class).exportAsInputStream();
        try {
            return deployInternal(archive.getName(), input);
        } finally {
            if (input != null)
                try {
                    input.close();
                } catch (IOException e) {
                    log.warnf(e, "Failed to close resource %s", input);
                }
        }
    }

    private String deployInternal(String name, InputStream input) throws DeploymentException {
        try {
            return deployer.deploy(name, input);
        } catch (Exception ex) {
            Throwable rootCause = ex.getCause();
            while (null != rootCause) {
                rootCause = rootCause.getCause();
            }
            throw new DeploymentException("Cannot deploy: " + name, rootCause);
        }
    }
}