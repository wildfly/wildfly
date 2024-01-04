/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api.interceptor;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
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
