/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.jca.workmanager.distributed.ra;

import javax.naming.NamingException;
import javax.naming.Reference;
import jakarta.resource.Referenceable;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterAssociation;
import java.io.Serializable;

import org.jboss.logging.Logger;

/**
 * Note that since the implementation contains a {@link ResourceAdapter} field, and that field is
 * not serializable, the admin object can only be injected via {@link jakarta.annotation.Resource}.
 *
 * That means that the tests that you write can't use
 * {@link org.jboss.arquillian.container.test.api.RunAsClient} and look up the object remotely.
 *
 * What you can do, is deploy a Jakarta Enterprise Beans bean, that will report the statistics via its admin object. You
 * should be able to lookup that Jakarta Enterprise Beans and use it as a proxy to the resource adapter below. However,
 * the Jakarta Enterprise Beans has to be {@link jakarta.ejb.Stateless}, otherwise, once started in a cluster, a session is
 * going to be created for it which has to be serializable.
 */
public class DistributedAdminObject1Impl implements DistributedAdminObject1,
        ResourceAdapterAssociation, Referenceable, Serializable {

    private static final Logger log = Logger.getLogger(DistributedAdminObject1Impl.class.getSimpleName());
    private static final long serialVersionUID = 466880381773994922L;

    static {
        log.trace("Initializing DistributedAdminObject1Impl");
    }

    private ResourceAdapter ra;
    private Reference reference;
    private String name;

    public DistributedAdminObject1Impl() {
        setName("DistributedAdminObject1Impl");
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ResourceAdapter getResourceAdapter() {
        return ra;
    }

    public void setResourceAdapter(ResourceAdapter ra) {
        this.ra = ra;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reference getReference() throws NamingException {
        return reference;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 17;
        if (name != null) { result += 31 * result + 7 * name.hashCode(); } else { result += 31 * result + 7; }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (!(other instanceof DistributedAdminObject1Impl)) { return false; }
        DistributedAdminObject1Impl obj = (DistributedAdminObject1Impl) other;
        boolean result = true;
        if (result) {
            if (name == null) { result = obj.getName() == null; } else { result = name.equals(obj.getName()); }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.getClass().toString() + "name=" + name;
    }
}
