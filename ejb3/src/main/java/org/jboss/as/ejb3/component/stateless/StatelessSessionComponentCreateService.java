/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateless;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.session.SessionBeanComponentCreateService;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.ejb.client.Affinity;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Stuart Douglas
 */
public class StatelessSessionComponentCreateService extends SessionBeanComponentCreateService {

    private final InjectedValue<PoolConfig> poolConfig = new InjectedValue<>();

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public StatelessSessionComponentCreateService(final ComponentConfiguration componentConfiguration, final ApplicationExceptions ejbJarConfiguration) {
        super(componentConfiguration, ejbJarConfiguration);
    }

    @Override
    protected BasicComponent createComponent() {
        return new StatelessSessionComponent(this);
    }

    public PoolConfig getPoolConfig() {
        return this.poolConfig.getOptionalValue();
    }

    public Injector<PoolConfig> getPoolConfigInjector() {
        return this.poolConfig;
    }

    public Affinity getWeakAffinity() {
        return Affinity.NONE;
    }

}
