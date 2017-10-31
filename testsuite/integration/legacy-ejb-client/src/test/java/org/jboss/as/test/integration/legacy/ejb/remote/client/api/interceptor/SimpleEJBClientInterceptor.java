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

package org.jboss.as.test.integration.legacy.ejb.remote.client.api.interceptor;

import java.util.Map;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;

/**
 * An EJB client side interceptor
 *
 * @author Jaikiran Pai
 */
public class SimpleEJBClientInterceptor implements EJBClientInterceptor {

    private final Map<String, Object> data;

    SimpleEJBClientInterceptor(final Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public void handleInvocation(EJBClientInvocationContext context) throws Exception {
        // add all the data to the EJB client invocation context so that it becomes available to the server side
        context.getContextData().putAll(data);
        // proceed "down" the invocation chain
        context.sendRequest();
    }

    @Override
    public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
        // we don't have anything special to do with the result so just return back the result
        // "up" the invocation chain
        return context.getResult();
    }
}
