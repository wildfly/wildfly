/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.resourceadapter;

/**
 * @author Jaikiran Pai
 */
public class ResourceAdapterDeploymentTracker {

    public static final ResourceAdapterDeploymentTracker INSTANCE = new ResourceAdapterDeploymentTracker();

    private boolean endpointActivationCalled;
    private boolean endpointDeactivationCalled;
    private boolean endpointStartCalled;
    private boolean endpointStopCalled;

    private ResourceAdapterDeploymentTracker() {

    }

    void endpointActivationCalled() {
        this.endpointActivationCalled = true;
    }

    public boolean wasEndpointActivationCalled() {
        return this.endpointActivationCalled;
    }

    void endpointDeactivationCalled() {
        this.endpointDeactivationCalled = true;
    }

    public boolean wasEndpointDeactivationCalled() {
        return this.endpointDeactivationCalled;
    }

    void endpointStartCalled() {
        this.endpointStartCalled = true;
    }

    public boolean wasEndpointStartCalled() {
        return this.endpointStartCalled;
    }

    void endpointStopCalled() {
        this.endpointStopCalled = true;
    }

    public boolean wasEndpointStopCalled() {
        return this.endpointStopCalled;
    }
}
