/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.jca.rar;

import org.jboss.logging.Logger;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;

import org.jboss.as.connector.services.bootstrap.NamedBootstrapContext;
import org.jboss.as.connector.services.workmanager.NamedWorkManager;

/**
 * MultipleResourceAdapter2
 */
public class MultipleResourceAdapter2 implements ResourceAdapter {

    /**
     * The logger
     */
    private static Logger log = Logger.getLogger("MultipleResourceAdapter2");

    /**
     * Name
     */
    private String name;

    private String bootstrapContextName = "undefined";

    public void setBootstrapContextName(String bootstrapContextName) {
        this.bootstrapContextName = bootstrapContextName;
    }

    public void setWorkManagerName(String workManagerName) {
        this.workManagerName = workManagerName;
    }

    private String workManagerName = "undefined";

    /**
     * Default constructor
     */
    public MultipleResourceAdapter2() {

    }

    /**
     * Set name
     *
     * @param name The value
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get name
     *
     * @return The value
     */
    public String getName() {
        return name;
    }

    public String getBootstrapContextName() {
        return bootstrapContextName;
    }

    public String getWorkManagerName() {
        return workManagerName;
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

        if (ctx instanceof NamedBootstrapContext) {
            NamedBootstrapContext nc = (NamedBootstrapContext) ctx;
            setBootstrapContextName(nc.getName());
            log.trace("Bootstrap-context:" + nc.getName());
        }
        WorkManager wm = ctx.getWorkManager();

        if (wm instanceof NamedWorkManager) {
            NamedWorkManager nw = (NamedWorkManager) wm;
            setWorkManagerName(nw.getName());
            log.trace("Work-manager:" + nw.getName());
        }

        Work myWork1 = new MultipleWork();
        Work myWork2 = new MultipleWork();
        Work myWork3 = new MultipleWork();

        try {
            wm.doWork(myWork1);
            wm.scheduleWork(myWork2);
            wm.startWork(myWork3);
        } catch (Exception e) {
            e.printStackTrace();
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
        if (name != null) { result += 31 * result + 7 * name.hashCode(); } else { result += 31 * result + 7; }
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
        if (!(other instanceof MultipleResourceAdapter2)) { return false; }
        MultipleResourceAdapter2 obj = (MultipleResourceAdapter2) other;
        boolean result = true;
        if (result) {
            if (name == null) { result = obj.getName() == null; } else { result = name.equals(obj.getName()); }
        }
        return result;
    }

}
