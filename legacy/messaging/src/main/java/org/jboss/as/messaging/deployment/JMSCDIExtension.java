/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.deployment;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionTarget;

import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.weld.injection.ForwardingInjectionTarget;

/**
 * CDI extension to provide injection of JMSContext resources.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class JMSCDIExtension implements Extension {

    private final PropertyReplacer propertyReplacer;

    public JMSCDIExtension(PropertyReplacer propertyReplacer) {
        this.propertyReplacer = propertyReplacer;
    }

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        AnnotatedType<JMSContextProducer> producer = bm.createAnnotatedType(JMSContextProducer.class);
        bbd.addAnnotatedType(producer);
    }

    public void wrapInjectionTarget(@Observes ProcessInjectionTarget<JMSContextProducer> event)
    {
        final InjectionTarget<JMSContextProducer> injectionTarget = event.getInjectionTarget();
        event.setInjectionTarget(new ForwardingInjectionTarget<JMSContextProducer>() {

            @Override
            public void inject(JMSContextProducer instance, CreationalContext<JMSContextProducer> ctx) {
                super.inject(instance, ctx);
                instance.setPropertyReplacer(propertyReplacer);
            }

            @Override
            protected InjectionTarget<JMSContextProducer> delegate() {
                return injectionTarget;
            }
        });
    }
}
