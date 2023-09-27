/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.arquillian;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.Container.State;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.logging.Logger;
import org.kohsuke.MetaInfServices;

/**
 * Arquillian extension which stops custom containers after testsuite run.
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @author Radoslav Husar
 */
@MetaInfServices(LoadableExtension.class)
public class StopCustomContainersOnAfterSuiteExtension implements LoadableExtension {

    static final Logger log = Logger.getLogger(StopCustomContainersOnAfterSuiteExtension.class);

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(StopCustomContainers.class);
    }

    public static class StopCustomContainers {
        public void close(@Observes AfterSuite event, ContainerRegistry registry) {
            for (Container c: registry.getContainers()) {
                if (c.getState() == State.STARTED && "custom".equalsIgnoreCase(c.getContainerConfiguration().getMode())) {
                    try {
                        log.tracef("Stopping custom container %s", c.getName());
                        // TODO workaround https://issues.jboss.org/browse/WFARQ-47
                        c.stop();
                        log.tracef("Stopped custom container %s", c.getName());
                    } catch (LifecycleException e) {
                        log.errorf("Failed to stop custom container %s: %s", c.getName(), e);
                    }
                }
            }
        }
    }
}
