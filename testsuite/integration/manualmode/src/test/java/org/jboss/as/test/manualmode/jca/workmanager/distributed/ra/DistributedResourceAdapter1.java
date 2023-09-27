/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.jca.workmanager.distributed.ra;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;

import org.jboss.jca.core.workmanager.DistributedWorkManagerImpl;

/**
 * DistributedResourceAdapter1 for use with DistributableWorkManager.
 *
 * In conjunction with {@link DistributedAdminObject1}, this class allows the tester to create workloads on
 * the servers as well as obtain statistics on where that workload was executed.
 */
public class DistributedResourceAdapter1 implements ResourceAdapter {

    private DistributedWorkManagerImpl dwm;

    public DistributedResourceAdapter1() {
        // empty
    }

    public void setDwm(DistributedWorkManagerImpl dwm) {
        this.dwm = dwm;
    }

    public DistributedWorkManagerImpl getDwm() {
        return dwm;
    }

    /**
     * This is called during the activation of a message endpoint.
     *
     * @param endpointFactory A message endpoint factory instance.
     * @param spec            An activation spec JavaBean instance.
     * @throws ResourceException generic exception
     */
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
    }

    /**
     * This is called when a message endpoint is deactivated.
     *
     * @param endpointFactory A message endpoint factory instance.
     * @param spec            An activation spec JavaBean instance.
     */
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
    }

    /**
     * This is called when a resource adapter instance is bootstrapped.
     *
     * @param ctx A bootstrap context containing references
     * @throws ResourceAdapterInternalException indicates bootstrap failure.
     */
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        WorkManager wm = ctx.getWorkManager();
        if (wm instanceof DistributedWorkManagerImpl) {
            DistributedWorkManagerImpl dwm = (DistributedWorkManagerImpl) wm;
            setDwm(dwm);
        }
    }

    /**
     * This is called when a resource adapter instance is undeployed or during application server shutdown.
     */
    public void stop() {
    }

    /**
     * This method is called by the application server during crash recovery.
     *
     * @param specs An array of ActivationSpec JavaBeans
     * @return An array of XAResource objects
     * @throws ResourceException generic exception
     */
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return 17;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (!(other instanceof DistributedResourceAdapter1)) { return false; }
        DistributedResourceAdapter1 obj = (DistributedResourceAdapter1) other;
        boolean result;
        if (dwm == null) { result = obj.getDwm() == null; } else { result = dwm.equals(obj.getDwm()); }
        return result;
    }

}
