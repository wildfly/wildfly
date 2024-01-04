/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming;

import org.jboss.msc.service.ServiceName;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import java.util.Hashtable;

/**
 * @author John Bailey
 */
public class ManagedReferenceObjectFactory extends ServiceReferenceObjectFactory {

    public static Reference createReference(final ServiceName serviceName) {
        return ServiceReferenceObjectFactory.createReference(serviceName, ManagedReferenceObjectFactory.class);
    }

    public Object getObjectInstance(final Object serviceValue, final Object obj, final Name name, final Context nameCtx, final Hashtable<?, ?> environment) throws Exception {
        final ManagedReferenceFactory managedReferenceFactory = ManagedReferenceFactory.class.cast(serviceValue);
        return managedReferenceFactory.getReference().getInstance();
    }
}
