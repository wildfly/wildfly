/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api.interceptor;

import org.jboss.ejb.client.EJBClientContext;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;
import java.util.HashMap;
import java.util.Map;

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
        // create some data that the client side interceptor will pass along during the EJB invocation
        this.interceptorData = new HashMap<String, Object>();
        final String keyOne = "abc";
        final Object valueOne = "def";
        final String keyTwo = "blah";
        final Object valueTwo = new Integer("12");

        interceptorData.put(keyOne, valueOne);
        interceptorData.put(keyTwo, valueTwo);

        final SimpleEJBClientInterceptor clientInterceptor = new SimpleEJBClientInterceptor(interceptorData);
        // get hold of the EJBClientContext and register the client side interceptor
        ejbClientContext = EJBClientContext.getCurrent().withAddedInterceptors(clientInterceptor);
    }

    @Override
    public Map<String, Object> invokeRemoteViewAndGetInvocationData(final String... key) {
        try {
            return ejbClientContext.runCallable(() -> remoteViewSFSB.getInvocationData(key));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, Object> getDataSetupForInvocationContext() {
        return this.interceptorData;
    }
}
