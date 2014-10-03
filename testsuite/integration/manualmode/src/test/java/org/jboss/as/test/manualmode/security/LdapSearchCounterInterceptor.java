/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.security;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;

/**
 * ApacheDS interceptor which implements a counter lookup and search operations. The counter is in static variable so it's
 * shared accross all ApacheDS instances.
 * 
 * @author Josef Cacek
 */
public class LdapSearchCounterInterceptor extends BaseInterceptor {

    private static volatile long counter = 0L;

    /**
     * Increases counter by 1 and returns to the next interceptor.
     * 
     * @see org.apache.directory.server.core.api.interceptor.BaseInterceptor#lookup(org.apache.directory.server.core.api.interceptor.context.LookupOperationContext)
     */
    @Override
    public Entry lookup(LookupOperationContext lookupContext) throws LdapException {
        counter++;
        return next(lookupContext);
    }

    /**
     * Increases counter by 1 and returns to the next interceptor.
     * 
     * @see org.apache.directory.server.core.api.interceptor.BaseInterceptor#search(org.apache.directory.server.core.api.interceptor.context.SearchOperationContext)
     */
    @Override
    public EntryFilteringCursor search(SearchOperationContext searchContext) throws LdapException {
        counter++;
        return next(searchContext);
    }

    /**
     * Resets counter to zero.
     */
    public static synchronized void resetCounter() {
        counter = 0L;
    }

    /**
     * Returns the current counter value.
     * 
     * @return counter
     */
    public static long getCounter() {
        return counter;
    }
}