package org.jboss.as.jaxrs.deployment;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

import javax.ws.rs.core.Application;
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
    private Class<? extends Application> scannedApplicationClass;
    private boolean bootClasses;
    private boolean unwrappedExceptionsParameterSet;
    private final Set<String> scannedJndiComponentResources = new LinkedHashSet<String>();

    /**
     * Merges a list of additional JAX-RS deployment data with this lot of deployment data.
     *
     * @param deploymentData
     */
    public void merge(final List<ResteasyDeploymentData> deploymentData) throws DeploymentUnitProcessingException {
        Class<? extends Application> application = null;
        for (ResteasyDeploymentData data : deploymentData) {
            if (!dispatcherCreated && scannedApplicationClass == null) {
                if (data.getScannedApplicationClass() != null) {
                    if (application != null) {
                        throw new DeploymentUnitProcessingException("More than one Application class found in deployment " + application + " and " + data.getScannedApplicationClass());
                    }
                    application = data.getScannedApplicationClass();
                }
            }
            if (scanResources) {
                scannedResourceClasses.addAll(data.getScannedResourceClasses());
                scannedJndiComponentResources.addAll(data.getScannedJndiComponentResources());
            }
            if (scanProviders) {
                scannedProviderClasses.addAll(data.getScannedProviderClasses());
            }
        }
        if (scannedApplicationClass == null) {
            scannedApplicationClass = application;
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

    public Class<? extends Application> getScannedApplicationClass() {
        return scannedApplicationClass;
    }

    public void setScannedApplicationClass(Class<? extends Application> scannedApplicationClass) {
        this.scannedApplicationClass = scannedApplicationClass;
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
