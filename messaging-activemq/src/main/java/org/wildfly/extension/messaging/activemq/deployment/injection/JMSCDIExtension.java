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

package org.wildfly.extension.messaging.activemq.deployment.injection;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import org.jboss.metadata.property.PropertyReplacer;

/**
 * CDI extension to provide injection of JMSContext resources.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
class JMSCDIExtension implements Extension {

    static PropertyReplacer propertyReplacer;

    JMSCDIExtension(PropertyReplacer propertyReplacer) {
        // store the propertyReplacer in a static field so that it can be used in JMSInfo by beans instantiated by CDI
        JMSCDIExtension.propertyReplacer = propertyReplacer;
    }

    private void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        AnnotatedType<RequestedJMSContext> requestedContextBean = bm.createAnnotatedType(RequestedJMSContext.class);
        bbd.addAnnotatedType(requestedContextBean, JMSCDIExtension.class.getName() + "-" + RequestedJMSContext.class.getName());
        AnnotatedType<TransactedJMSContext> transactedContextBean = bm.createAnnotatedType(TransactedJMSContext.class);
        bbd.addAnnotatedType(transactedContextBean, JMSCDIExtension.class.getName() + "-" + TransactedJMSContext.class.getName());
        AnnotatedType<InjectedJMSContext> contextBean = bm.createAnnotatedType(InjectedJMSContext.class);
        bbd.addAnnotatedType(contextBean, JMSCDIExtension.class.getName() + "-" + InjectedJMSContext.class.getName());
    }
}
