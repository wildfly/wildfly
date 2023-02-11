/*
 * JBoss, Home of Professional Open Source
 * Copyright 2023, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import javax.naming.Name;
import javax.naming.NamingException;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

/**
 * A naming interceptor applied before the name is being resolved by the InitialContext
 */
public interface LookupInterceptor {

    /**
     * Handles the naming invocation.
     *
     * @param name the name being resolved
     * @throws NamingException if an invocation error occurs
     */
    public void aroundLookup(Name name) throws NamingException;
}
