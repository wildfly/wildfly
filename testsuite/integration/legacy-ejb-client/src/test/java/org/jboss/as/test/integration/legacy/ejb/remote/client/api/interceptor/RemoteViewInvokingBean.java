/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateful;

import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientInterceptor;

/**
 * @author Jaikiran Pai
 */
@Stateful(passivationCapable = false)
@Remote(RemoteViewInvoker.class)
public class RemoteViewInvokingBean implements RemoteViewInvoker {

    @EJB
    private RemoteSFSB remoteViewSFSB;

    private Map<String, Object> interceptorData;

    private EJBClientContext ejbClientContext;


    @PostConstruct
    void setupClientInterceptor() {
        // get hold of the EJBClientContext
        this.ejbClientContext = EJBClientContext.requireCurrent();

        // create some data that the client side interceptor will pass along during the EJB invocation
        this.interceptorData = new HashMap<String, Object>();
        final String keyOne = "abc";
        final Object valueOne = "def";
        final String keyTwo = "blah";
        final Object valueTwo = new Integer("12");

        interceptorData.put(keyOne, valueOne);
        interceptorData.put(keyTwo, valueTwo);

        final SimpleEJBClientInterceptor clientInterceptor = new SimpleEJBClientInterceptor(interceptorData);
        // register the client side interceptor
        try {
            Method withAddedInterceptors = ejbClientContext.getClass().getMethod("withAddedInterceptors", EJBClientInterceptor[].class);
            this.ejbClientContext = (EJBClientContext) withAddedInterceptors.invoke(ejbClientContext,  (Object) (new EJBClientInterceptor[] {clientInterceptor}));

        } catch (Throwable t) {
            throw new IllegalStateException("Could not invoke method", t);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> invokeRemoteViewAndGetInvocationData(final String... key) {
        Callable<Map<String, Object>> callable = () -> remoteViewSFSB.getInvocationData(key);
        try {
            return (Map<String, Object>)ejbClientContext.getClass().getMethod("runCallable", Callable.class).invoke(ejbClientContext, callable);
        } catch (Throwable t) {
            throw new IllegalStateException("Could not invoke callable", t);
        }

    }

    @Override
    public Map<String, Object> getDataSetupForInvocationContext() {
        return this.interceptorData;
    }
}
