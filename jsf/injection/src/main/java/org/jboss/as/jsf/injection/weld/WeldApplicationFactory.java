/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsf.injection.weld;

import jakarta.faces.application.Application;
import jakarta.faces.application.ApplicationFactory;

/**
 * @author pmuir
 */
public class WeldApplicationFactory extends ForwardingApplicationFactory {

    private final ApplicationFactory applicationFactory;

    private volatile Application application;

    // This private constructor must never be called, but it is here to suppress the WELD-001529 warning
    // that an InjectionTarget is created for this class with no appropriate constructor.
    private WeldApplicationFactory() {
        super();
        applicationFactory = null;
    }

    public WeldApplicationFactory(ApplicationFactory applicationFactory) {
        this.applicationFactory = applicationFactory;
    }

    @Override
    protected ApplicationFactory delegate() {
        return applicationFactory;
    }

    @Override
    public Application getApplication() {
        if (application == null) {
            synchronized (this) {
                if (application == null) {
                    application = new WeldApplication(delegate().getApplication());
                }
            }
        }
        return application;
    }

    @Override
    public void setApplication(final Application application) {
        synchronized (this) {
            this.application = null;
            super.setApplication(application);
        }
    }
}
