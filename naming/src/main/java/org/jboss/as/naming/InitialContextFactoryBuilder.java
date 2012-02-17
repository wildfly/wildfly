/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming;

import static org.jboss.as.naming.NamingMessages.MESSAGES;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Initial context factory builder which ensures the proper naming context factory is used if the environment
 * does not override the initial context impl property.
 *
 * @author John E. Bailey
 */
public class InitialContextFactoryBuilder implements javax.naming.spi.InitialContextFactoryBuilder {
    private static final javax.naming.spi.InitialContextFactory DEFAULT_FACTORY = new InitialContextFactory();

    /**
     * Create a InitialContext factory.  If the environment does not override the factory class it will use the
     * default JBoss naming context factory.
     *
     * @param environment The environment
     * @return An initial context factory
     * @throws NamingException If an error occurs loading the factory class.
     */
    public javax.naming.spi.InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) throws NamingException {
        if (environment == null)
            environment = new Hashtable<String, Object>();

        final String factoryClassName = (String)environment.get(Context.INITIAL_CONTEXT_FACTORY);
        if(factoryClassName == null || InitialContextFactory.class.getName().equals(factoryClassName)) {
            return DEFAULT_FACTORY;
        }
        final ClassLoader classLoader = getContextClassLoader();
        try {
            final Class<?> factoryClass = Class.forName(factoryClassName, true, classLoader);
            return (javax.naming.spi.InitialContextFactory)factoryClass.newInstance();
        } catch (Exception e) {
            throw MESSAGES.failedToInstantiate("InitialContextFactory", factoryClassName, classLoader);
        }
    }

    private ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(
            new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            }
        );
    }

}
