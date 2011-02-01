/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component.service;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.naming.Context;
import org.jboss.as.ee.component.AbstractComponent;
import org.jboss.as.ee.component.Component;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service wrapper for a {@link Component}.
 *
 * @author John Bailey
 */
public class ComponentService implements Service<Component> {

    private final AtomicBoolean started = new AtomicBoolean();

    private final InjectedValue<Context> compContext = new InjectedValue<Context>();
    private final InjectedValue<Context> moduleContext = new InjectedValue<Context>();
    private final InjectedValue<Context> appContext = new InjectedValue<Context>();

    private final Component component;

    public ComponentService(final Component component) {
        this.component = component;
    }

    public void start(StartContext context) throws StartException {
        if (!started.compareAndSet(false, true)) {
            throw new StartException("Unable to start component.  Already started.");
        }
        if(component instanceof AbstractComponent) {
            final AbstractComponent abstractComponent = AbstractComponent.class.cast(component);
            abstractComponent.setComponentContext(compContext.getValue());
            abstractComponent.setModuleContext(moduleContext.getValue());
            abstractComponent.setApplicationContext(appContext.getValue());
        }
        component.start();
    }

    public void stop(StopContext context) {
        if (started.compareAndSet(true, false)) {
            component.stop();
        } else {
            throw new IllegalStateException("Unable to stop component.  Already stopped.");
        }
    }

    public Component getValue() throws IllegalStateException, IllegalArgumentException {
        if (!started.get()) {
            throw new IllegalStateException("Unable to retrieve component.  Service is stopped.");
        }
        return component;
    }


    public Injector<Context> getCompContextInjector() {
        return compContext;
    }

    public Injector<Context> getModuleContextInjector() {
        return moduleContext;
    }

    public Injector<Context> getAppContextInjector() {
        return appContext;
    }
}
