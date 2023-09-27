/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.security.jacc.context;

import jakarta.ejb.Stateless;
import jakarta.security.jacc.PolicyContext;
import jakarta.security.jacc.PolicyContextException;
import jakarta.servlet.http.HttpServletRequest;

@Stateless
public class PolicyContextTestBean {

    public HttpServletRequest getHttpServletRequestFromPolicyContext() throws PolicyContextException {
        return getHttpServletRequest();
    }

    // public as accessed from a different module.
    public static HttpServletRequest getHttpServletRequest() throws PolicyContextException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) PolicyContext
                .getContext("jakarta.servlet.http.HttpServletRequest");
        return httpServletRequest;
    }
}
