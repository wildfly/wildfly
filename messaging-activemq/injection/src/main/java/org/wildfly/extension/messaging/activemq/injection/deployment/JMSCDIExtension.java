/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.injection.deployment;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

import org.jboss.metadata.property.PropertyReplacer;

/**
 * Jakarta Contexts and Dependency Injection extension to provide injection of JMSContext resources.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
class JMSCDIExtension implements Extension {

    static PropertyReplacer propertyReplacer;

    JMSCDIExtension(PropertyReplacer propertyReplacer) {
        // store the propertyReplacer in a static field so that it can be used in JMSInfo by beans instantiated by Jakarta Contexts and Dependency Injection
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
