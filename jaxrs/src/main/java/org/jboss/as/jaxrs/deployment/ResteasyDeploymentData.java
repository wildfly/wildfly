/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jaxrs.deployment;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

import javax.ws.rs.core.Application;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class ResteasyDeploymentData {
    private boolean scanAll;
    private boolean scanResources;
    private boolean scanProviders;
    private boolean dispatcherCreated;
    private final Set<String> scannedResourceClasses = new LinkedHashSet<String>();
    private final Set<String> scannedProviderClasses = new LinkedHashSet<String>();
    private List<Class<? extends Application>> scannedApplicationClasses = new ArrayList<>();
    private boolean bootClasses;
    private boolean unwrappedExceptionsParameterSet;
    private final Set<String> scannedJndiComponentResources = new LinkedHashSet<String>();

    /**
     * Merges a list of additional JAX-RS deployment data with this lot of deployment data.
     *
     * @param deploymentData
     */
    public void merge(final List<ResteasyDeploymentData> deploymentData) throws DeploymentUnitProcessingException {
        for (ResteasyDeploymentData data : deploymentData) {
            scannedApplicationClasses.addAll(data.getScannedApplicationClasses());
            if (scanResources) {
                scannedResourceClasses.addAll(data.getScannedResourceClasses());
                scannedJndiComponentResources.addAll(data.getScannedJndiComponentResources());
            }
            if (scanProviders) {
                scannedProviderClasses.addAll(data.getScannedProviderClasses());
            }
        }
    }


    public Set<String> getScannedJndiComponentResources() {
        return scannedJndiComponentResources;
    }

    public boolean isDispatcherCreated() {
        return dispatcherCreated;
    }

    public void setDispatcherCreated(boolean dispatcherCreated) {
        this.dispatcherCreated = dispatcherCreated;
    }

    public List<Class<? extends Application>> getScannedApplicationClasses() {
        return scannedApplicationClasses;
    }

    public boolean hasBootClasses() {
        return bootClasses;
    }

    public void setBootClasses(boolean bootClasses) {
        this.bootClasses = bootClasses;
    }

    public boolean shouldScan() {
        return scanAll || scanResources || scanProviders;
    }

    public boolean isScanAll() {
        return scanAll;
    }

    public void setScanAll(boolean scanAll) {
        if (scanAll) {
            scanResources = true;
            scanProviders = true;
        }
        this.scanAll = scanAll;
    }

    public boolean isScanResources() {
        return scanResources;
    }

    public void setScanResources(boolean scanResources) {
        this.scanResources = scanResources;
    }

    public boolean isScanProviders() {
        return scanProviders;
    }

    public void setScanProviders(boolean scanProviders) {
        this.scanProviders = scanProviders;
    }

    public Set<String> getScannedResourceClasses() {
        return scannedResourceClasses;
    }

    public Set<String> getScannedProviderClasses() {
        return scannedProviderClasses;
    }

    public boolean isUnwrappedExceptionsParameterSet() {
        return unwrappedExceptionsParameterSet;
    }

    public void setUnwrappedExceptionsParameterSet(boolean unwrappedExceptionsParameterSet) {
        this.unwrappedExceptionsParameterSet = unwrappedExceptionsParameterSet;
    }
}
