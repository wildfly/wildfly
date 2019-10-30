/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.jsf.injection.weld;

import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;

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
