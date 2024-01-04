/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.interfaces.java;

import org.jboss.as.naming.NamingContext;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * Implementation of {@code ObjectFactory} used to create a {@code NamingContext} instances to support the java: namespace.
 *
 * @author John E. Bailey
 */
public class javaURLContextFactory implements ObjectFactory {

    /** {@inheritDoc} */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        return new NamingContext(name != null ? name : new CompositeName(""), environment);
    }
}
