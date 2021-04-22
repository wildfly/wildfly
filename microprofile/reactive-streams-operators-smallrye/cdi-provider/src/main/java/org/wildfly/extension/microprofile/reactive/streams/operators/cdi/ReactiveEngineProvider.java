/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.reactive.streams.operators.cdi;

import static org.wildfly.extension.microprofile.reactive.streams.operators.cdi._private.MicroProfileReactiveStreamsOperatorsCdiProviderLogger.LOGGER;

import java.util.Iterator;
import java.util.ServiceLoader;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

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
        Iterator<ReactiveStreamsEngine> iterator = ServiceLoader.load(ReactiveStreamsEngine.class).iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        throw LOGGER.noImplementationFound(ReactiveStreamsEngine.class.getName());
    }
}
