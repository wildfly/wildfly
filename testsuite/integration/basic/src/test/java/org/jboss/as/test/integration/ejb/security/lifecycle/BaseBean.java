/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.lifecycle;

import jakarta.ejb.EJBContext;

/**
 * The base class for calling the four EJBContext methods.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class BaseBean {

    private static final String DEFAULT_ROLE = "Users";

    public static final String GET_CALLER_PRINCIPAL = "getCallerPrincipal()";

    public static final String IS_CALLER_IN_ROLE = "isCallerInRole(String)";

    public static final String DEPENDENCY_INJECTION = "Dependency Injection";

    public static final String LIFECYCLE_CALLBACK = "Lifecycle Callback";

    public static final String BUSINESS = "Business";

    public static final String AFTER_BEGIN = "After Begin";

    public abstract EJBContext getEJBContext();

    private String callGetCallerPrincipal() {
        try {
            return getEJBContext().getCallerPrincipal().getName();
        } catch (Exception e) {
            return e.getClass().getSimpleName();
        }
    }

    private String callIsCallerInRole() {
        try {
            return Boolean.toString(getEJBContext().isCallerInRole(DEFAULT_ROLE));
        } catch (Exception e) {
            return e.getClass().getSimpleName();
        }
    }

    void performTests(final String beanMethod) {
        ResultHolder.addResult(beanMethod, GET_CALLER_PRINCIPAL, callGetCallerPrincipal());
        ResultHolder.addResult(beanMethod, IS_CALLER_IN_ROLE,callIsCallerInRole());
    }

}
