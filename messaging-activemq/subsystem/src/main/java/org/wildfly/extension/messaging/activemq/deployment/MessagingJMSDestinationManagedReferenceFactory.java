/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.deployment;

import jakarta.jms.Destination;

import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.msc.service.Service;

/**
* @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
*/
class MessagingJMSDestinationManagedReferenceFactory<D extends Destination> implements ContextListAndJndiViewManagedReferenceFactory {

    private final Service<D> service;

    public MessagingJMSDestinationManagedReferenceFactory(Service<D> service) {
        this.service = service;
    }

    @Override
    public String getJndiViewInstanceValue() {
        return String.valueOf(getReference().getInstance());
    }

    @Override
    public String getInstanceClassName() {
        final Object value = getReference().getInstance();
        return value != null ? value.getClass().getName() : ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
    }

    @Override
    public ManagedReference getReference() {
        return new ValueManagedReference(service.getValue());
    }
}
