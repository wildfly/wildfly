package org.jboss.as.jaxrs.deployment;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class ResteasyDeploymentData {
    private boolean scanAll;
    private boolean scanResources;
    private boolean scanProviders;
    private boolean dispatcherCreated;
    private Set<String> scannedResourceClasses = new LinkedHashSet<String>();
    private Set<String> scannedProviderClasses = new LinkedHashSet<String>();
    private Class<? extends Application> scannedApplicationClass;
    private boolean bootClasses;
    private boolean unwrappedExceptionsParameterSet;
    private Set<String> scannedJndiComponentResources = new LinkedHashSet<String>();

    public Set<String> getScannedJndiComponentResources() {
        return scannedJndiComponentResources;
    }

    public void setScannedJndiComponentResources(Set<String> scannedJndiComponentResources) {
        this.scannedJndiComponentResources = scannedJndiComponentResources;
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

    public void setScannedResourceClasses(Set<String> scannedResourceClasses) {
        this.scannedResourceClasses = scannedResourceClasses;
    }

    public Set<String> getScannedProviderClasses() {
        return scannedProviderClasses;
    }

    public void setScannedProviderClasses(Set<String> scannedProviderClasses) {
        this.scannedProviderClasses = scannedProviderClasses;
    }

    public boolean isUnwrappedExceptionsParameterSet() {
        return unwrappedExceptionsParameterSet;
    }

    public void setUnwrappedExceptionsParameterSet(boolean unwrappedExceptionsParameterSet) {
        this.unwrappedExceptionsParameterSet = unwrappedExceptionsParameterSet;
    }
}
