/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
