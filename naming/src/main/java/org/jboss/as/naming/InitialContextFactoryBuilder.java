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

import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * Initial context factory builder which ensures the proper naming context factory.
 *
 * @author John E. Bailey
 * @author Eduardo Martins
 */
public class InitialContextFactoryBuilder implements javax.naming.spi.InitialContextFactoryBuilder {

    private static final javax.naming.spi.InitialContextFactory DEFAULT_FACTORY = new InitialContextFactory();

    /**
     * Retrieves the default JBoss naming context factory.
     *
     * @param environment The environment
     * @return An initial context factory
     * @throws NamingException If an error occurs loading the factory class.
     */
    public javax.naming.spi.InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) throws NamingException {
        // return our default factory, the initial context it creates must be the responsible for handling a
        // custom initial context factory in env, to ensure that URL factories are processed first, as the JDK does
        return DEFAULT_FACTORY;
    }
}
