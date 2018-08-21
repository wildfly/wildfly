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
import io.smallrye.opentracing.SmallRyeTracingCDIInterceptor;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

public class TracingCDIExtension implements Extension {

    public void observeBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager manager) {
        String extensionName = TracingCDIExtension.class.getName();
        bbd.addAnnotatedType(manager.createAnnotatedType(TracerProducer.class), extensionName + "-" + TracerProducer.class.getName());
        bbd.addAnnotatedType(manager.createAnnotatedType(SmallRyeTracingCDIInterceptor.class), extensionName + "-" + SmallRyeTracingCDIInterceptor.class.getName());
    }

    public void skipTracerBeans(@Observes ProcessAnnotatedType<? extends Tracer> processAnnotatedType) {
        TracingLogger.ROOT_LOGGER.extraTracerBean(processAnnotatedType.getAnnotatedType().getJavaClass().getName());
        processAnnotatedType.veto();
    }
}
