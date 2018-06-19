/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
