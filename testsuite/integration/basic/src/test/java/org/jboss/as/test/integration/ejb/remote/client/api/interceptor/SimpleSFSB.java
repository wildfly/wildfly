/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.remote.client.api.interceptor;

import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jaikiran Pai
 */
@Stateful(passivationCapable = false)
@Remote(RemoteSFSB.class)
public class SimpleSFSB implements RemoteSFSB {

    private Map<String, Object> invocationData;

    @Override
    public Map<String, Object> getInvocationData(final String... keys) {
        // return the data that was requested for the passed keys
        final Map<String, Object> subset = new HashMap<String, Object>();
        for (final String key : keys) {
            subset.put(key, invocationData.get(key));
        }
        return subset;
    }

    @AroundInvoke
    private Object aroundInvoke(final InvocationContext invocationContext) throws Exception {
        // keep track of the context data that was passed during this invocation
        this.invocationData = invocationContext.getContextData();
        return invocationContext.proceed();
    }
}
