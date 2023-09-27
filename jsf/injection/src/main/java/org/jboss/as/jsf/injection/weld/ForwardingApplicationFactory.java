/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsf.injection.weld;

import jakarta.faces.application.Application;
import jakarta.faces.application.ApplicationFactory;

/**
 * @author pmuir
 *
 */
public abstract class ForwardingApplicationFactory extends ApplicationFactory {

    protected abstract ApplicationFactory delegate();

    @Override
    public Application getApplication() {
        return delegate().getApplication();
    }

    @Override
    public void setApplication(Application application) {
        delegate().setApplication(application);
    }

    @Override
    public boolean equals(Object obj) {
        return delegate().equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public String toString() {
        return delegate().toString();
    }

    @Override
    public ApplicationFactory getWrapped() {
        return delegate();
    }
}