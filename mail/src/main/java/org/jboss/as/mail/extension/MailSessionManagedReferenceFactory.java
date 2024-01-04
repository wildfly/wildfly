/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import java.util.function.Supplier;

import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;

/**
* @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
*/
class MailSessionManagedReferenceFactory implements ContextListAndJndiViewManagedReferenceFactory {

    private final Supplier<SessionProvider> provider;

    public MailSessionManagedReferenceFactory(Supplier<SessionProvider> provider) {
        this.provider = provider;
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
        return new ValueManagedReference(this.provider.get().getSession());
    }
}
