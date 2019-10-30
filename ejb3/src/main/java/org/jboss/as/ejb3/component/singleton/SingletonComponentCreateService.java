/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.singleton;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.as.ejb3.component.session.SessionBeanComponentCreateService;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

import java.util.List;

/**
 * @author Stuart Douglas
 */
public class SingletonComponentCreateService extends SessionBeanComponentCreateService {

    private final boolean initOnStartup;
    private final List<ServiceName> dependsOn;
    private final InjectedValue<DefaultAccessTimeoutService> defaultAccessTimeoutService = new InjectedValue<DefaultAccessTimeoutService>();

    public SingletonComponentCreateService(final ComponentConfiguration componentConfiguration, final ApplicationExceptions ejbJarConfiguration, final boolean initOnStartup, final List<ServiceName> dependsOn) {
        super(componentConfiguration, ejbJarConfiguration);
        this.initOnStartup = initOnStartup;
        this.dependsOn = dependsOn;
    }

    @Override
    protected BasicComponent createComponent() {
        return new SingletonComponent(this, dependsOn);
    }

    public boolean isInitOnStartup() {
        return this.initOnStartup;
    }

    public DefaultAccessTimeoutService getDefaultAccessTimeoutService() {
        return defaultAccessTimeoutService.getValue();
    }

    Injector<DefaultAccessTimeoutService> getDefaultAccessTimeoutInjector() {
        return this.defaultAccessTimeoutService;
    }
}
