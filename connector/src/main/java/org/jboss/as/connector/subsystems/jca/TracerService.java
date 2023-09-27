/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.jca;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A ResourceAdaptersService.
 *
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 */
final class TracerService implements Service<TracerService.Tracer> {


    private final Tracer value;
    private final InjectedValue<JcaSubsystemConfiguration> jcaConfig = new InjectedValue<JcaSubsystemConfiguration>();


    /**
     * create an instance *
     */
    public TracerService(Tracer value) {
        this.value = value;
    }

    @Override
    public Tracer getValue() throws IllegalStateException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        jcaConfig.getValue().setTracer(value.isEnabled());

    }

    @Override
    public void stop(StopContext context) {

    }

    public Injector<JcaSubsystemConfiguration> getJcaConfigInjector() {
        return jcaConfig;
    }

    public static class Tracer {
        private final boolean enabled;

        public Tracer(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

    }
}
