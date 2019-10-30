/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jsf.injection.weld.legacy;

import org.jboss.as.jsf.injection.weld.ForwardingApplicationFactory;

import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;

/**
 * @author <a href="ingo@redhat.com">Ingo Weiss</a>
 *
 * This is a copy of WeldApplicationFactory modified to use WeldApplicationLegacy instead
 */
public class WeldApplicationFactoryLegacy extends ForwardingApplicationFactory {

    private final ApplicationFactory applicationFactory;

    private volatile Application application;

    // This private constructor must never be called, but it is here to suppress the WELD-001529 warning
    // that an InjectionTarget is created for this class with no appropriate constructor.
    private WeldApplicationFactoryLegacy() {
        super();
        applicationFactory = null;
    }

    public WeldApplicationFactoryLegacy(ApplicationFactory applicationFactory) {
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
                    application = new WeldApplicationLegacy(delegate().getApplication());
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
