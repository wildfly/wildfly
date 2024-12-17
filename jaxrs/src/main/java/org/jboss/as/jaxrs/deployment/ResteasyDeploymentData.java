/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs.deployment;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

import jakarta.ws.rs.core.Application;
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
    private final Set<String> scannedResourceClasses = new LinkedHashSet<>();
    private final Set<String> scannedProviderClasses = new LinkedHashSet<>();
    private final List<Class<? extends Application>> scannedApplicationClasses = new ArrayList<>();
    private boolean bootClasses;
    private boolean unwrappedExceptionsParameterSet;
    private final Set<String> scannedJndiComponentResources = new LinkedHashSet<>();

    /**
     * Merges a list of additional Jakarta RESTful Web Services deployment data with this lot of deployment data.
     *
     * @param deploymentData the deployment data to merge
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
