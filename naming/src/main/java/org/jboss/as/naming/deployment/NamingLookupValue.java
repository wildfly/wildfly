/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.deployment;

import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Value that is looked up from a naming context.
 *
 * @author John E. Bailey
 */
public class NamingLookupValue<T> implements Value<T> {
    private final InjectedValue<Context> contextValue = new InjectedValue<Context>();
    private final String contextName;

    /**
     * Create a new instance.
     *
     * @param contextName The context name to lookup if the value is not injected
     */
    public NamingLookupValue(final String contextName) {
        this.contextName = contextName;
    }

    /**
     * Lookup the value from the naming context.
     *
     * @return the injected value if present, the value retrieved from the context if not.
     * @throws IllegalStateException The name is not found in the context when called
     */
    public T getValue() throws IllegalStateException {
        final Context context = contextValue.getValue();
        try {
            return (T)context.lookup(contextName);
        } catch (NamingException e) {
            throw NamingLogger.ROOT_LOGGER.entryNotRegistered(e, contextName, context);
        }
    }

    /**
     * Get the naming context injector.
     *
     * @return The context injector
     */
    public Injector<Context> getContextInjector() {
        return contextValue;
    }
}
