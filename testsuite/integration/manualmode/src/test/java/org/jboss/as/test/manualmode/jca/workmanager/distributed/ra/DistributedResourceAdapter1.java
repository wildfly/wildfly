/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.manualmode.jca.workmanager.distributed.ra;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;

import org.jboss.as.connector.services.workmanager.NamedDistributedWorkManager;

/**
 * DistributedResourceAdapter1 for use with DistributableWorkManager.
 *
 * In conjunction with {@link DistributedAdminObject1}, this class allows the tester to create workloads on
 * the servers as well as obtain statistics on where that workload was executed.
 */
public class DistributedResourceAdapter1 implements ResourceAdapter {

    private NamedDistributedWorkManager dwm;

    public DistributedResourceAdapter1() {
        // empty
    }

    public void setDwm(NamedDistributedWorkManager dwm) {
        this.dwm = dwm;
    }

    public NamedDistributedWorkManager getDwm() {
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

        if (wm instanceof NamedDistributedWorkManager) {
            NamedDistributedWorkManager ndwm = (NamedDistributedWorkManager) wm;
            setDwm(ndwm);
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
