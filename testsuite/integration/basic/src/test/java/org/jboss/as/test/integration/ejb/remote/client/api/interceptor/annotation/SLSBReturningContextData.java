/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api.interceptor.annotation;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * @author Jan Martiska
 */
@Stateless
public class SLSBReturningContextData implements SLSBReturningContextDataRemote {
    @Resource
    private SessionContext ctx;

    @Override
    public Object getContextData(String key) {
        return ctx.getContextData().get(key);
    }
}
