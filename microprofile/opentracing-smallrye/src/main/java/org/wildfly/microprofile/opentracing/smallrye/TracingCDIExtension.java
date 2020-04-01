/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.wildfly.microprofile.opentracing.smallrye;

import io.opentracing.Tracer;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

public class TracingCDIExtension implements Extension {

    private static final Map<ClassLoader, Tracer> TRACERS = Collections.synchronizedMap(new WeakHashMap<>());

    public static void registerApplicationTracer(ClassLoader classLoader, Tracer tracerInstance) {
        TRACERS.put(classLoader, tracerInstance);
    }

    public void registerTracerBean(@Observes AfterBeanDiscovery abd) {
        abd.addBean().addTransitiveTypeClosure(Tracer.class).produceWith(i -> {
            return TRACERS.get(Thread.currentThread().getContextClassLoader());
        });
    }

    public void skipTracerBeans(@Observes ProcessAnnotatedType<? extends Tracer> processAnnotatedType) {
        TracingLogger.ROOT_LOGGER.extraTracerBean(processAnnotatedType.getAnnotatedType().getJavaClass().getName());
        processAnnotatedType.veto();
    }

    /**
     * Called when the deployment is undeployed.
     *
     * @param bs
     */
    public void beforeShutdown(@Observes final BeforeShutdown bs) {
        TRACERS.remove(Thread.currentThread().getContextClassLoader());
    }
}
