/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming;

import javax.naming.Context;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * Initial context factory which returns {@code NamingContext} instances.
 *
 * @author John E. Bailey
 */
public class InitialContextFactory implements javax.naming.spi.InitialContextFactory {
    /**
     * Get an initial context instance.
     *
     * @param environment The naming environment
     * @return A naming context instance
     * @throws NamingException
     */
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return new InitialContext(environment);
    }
}
