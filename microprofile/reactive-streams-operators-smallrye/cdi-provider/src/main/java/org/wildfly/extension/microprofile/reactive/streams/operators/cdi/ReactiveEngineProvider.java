/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.streams.operators.cdi;

import static org.wildfly.extension.microprofile.reactive.streams.operators.cdi._private.CdiProviderLogger.LOGGER;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceLoader;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Dependent
public class ReactiveEngineProvider {

    /**
     * @return the reactive stream engine. It uses {@link ServiceLoader#load(Class)} to find an implementation from the
     *         Classpath.
     * @throws IllegalStateException if no implementations are found.
     */
    @Produces
    @ApplicationScoped
    public ReactiveStreamsEngine getEngine() {
        ReactiveStreamsEngine engine = AccessController.doPrivileged((PrivilegedAction<ReactiveStreamsEngine>) () -> {
            Iterator<ReactiveStreamsEngine> iterator = ServiceLoader.load(ReactiveStreamsEngine.class).iterator();
            if (iterator.hasNext()) {
                return iterator.next();
            }
            return null;
        });

        if (engine == null) {
            throw LOGGER.noImplementationFound(ReactiveStreamsEngine.class.getName());
        }
        return engine;
    }
}
