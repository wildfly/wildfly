/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.rar;

import org.jboss.logging.Logger;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;

import org.jboss.as.connector.services.workmanager.NamedWorkManager;

/**
 * MultipleResourceAdapter3
 */
public class MultipleResourceAdapter3 implements ResourceAdapter {

    /**
     * The logger
     */
    private static Logger log = Logger.getLogger("MultipleResourceAdapter3");


    /**
     * Default constructor
     */
    public MultipleResourceAdapter3() {

    }

    /**
     * Work manager
     */
    private NamedWorkManager workManager;

    /**
     * set WM
     *
     * @param workManagerName
     */
    public void setWorkManager(NamedWorkManager workManagerName) {
        this.workManager = workManagerName;
    }

    /**
     * get WM
     *
     * @return workManager
     */
    public NamedWorkManager getWorkManager() {
        return workManager;
    }

    /**
     * This is called during the activation of a message endpoint.
     *
     * @param endpointFactory A message endpoint factory instance.
     * @param spec            An activation spec JavaBean instance.
     * @throws ResourceException generic exception
     */
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
        log.trace("endpointActivation()");
    }

    /**
     * This is called when a message endpoint is deactivated.
     *
     * @param endpointFactory A message endpoint factory instance.
     * @param spec            An activation spec JavaBean instance.
     */
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        log.trace("endpointDeactivation()");
    }

    /**
     * This is called when a resource adapter instance is bootstrapped.
     *
     * @param ctx A bootstrap context containing references
     * @throws ResourceAdapterInternalException indicates bootstrap failure.
     */
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        log.trace("start()");

        WorkManager wm = ctx.getWorkManager();

        if (wm instanceof NamedWorkManager) {
            NamedWorkManager nw = (NamedWorkManager) wm;
            setWorkManager(nw);
            log.trace("Work-manager:" + nw);
        }

    }

    /**
     * This is called when a resource adapter instance is undeployed or during application server shutdown.
     */
    public void stop() {
        log.trace("stop()");
    }

    /**
     * This method is called by the application server during crash recovery.
     *
     * @param specs An array of ActivationSpec JavaBeans
     * @return An array of XAResource objects
     * @throws ResourceException generic exception
     */
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        log.trace("getXAResources()");
        return null;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        int result = 17;
        return result;
    }

    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param other The reference object with which to compare.
     * @return true if this object is the same as the obj argument, false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (!(other instanceof MultipleResourceAdapter3)) { return false; }
        MultipleResourceAdapter3 obj = (MultipleResourceAdapter3) other;
        boolean result = true;
        if (result) {
            if (workManager == null) { result = obj.getWorkManager() == null; } else { result = workManager.equals(obj.getWorkManager()); }
        }
        return result;
    }

}
