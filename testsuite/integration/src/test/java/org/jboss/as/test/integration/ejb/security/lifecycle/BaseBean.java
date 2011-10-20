/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.security.lifecycle;

import javax.ejb.EJBContext;

import java.security.Identity;

/**
 * The base class for calling the four EJBContext methods.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class BaseBean {

    private static final String DEFAULT_ROLE = "Users";

    public static final String GET_CALLER_PRINCIPAL = "getCallerPrincipal()";

    public static final String IS_CALLER_IN_ROLE = "isCallerInRole(String)";

    public static final String IS_CALLER_IN_ROLE_IDENITY = "isCallerInRole(Identity)";

    public static final String GET_CALLER_IDENTITY = "getCallerIdentity";

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

    private String callGetCallerIdentity() {
         try {
            return getEJBContext().getCallerIdentity().getName();
        } catch (Exception e) {
            return e.getClass().getSimpleName();
        }
    }

    private String callIsCallerInRole_Identity() {
        try {
            Identity id = new Identity(DEFAULT_ROLE) {
            };
            return Boolean.toString(getEJBContext().isCallerInRole(id));
        } catch (Exception e) {
            return e.getClass().getSimpleName();
        }
    }

    void performTests(final String beanMethod) {
        ResultHolder.addResult(beanMethod, GET_CALLER_PRINCIPAL, callGetCallerPrincipal());
        ResultHolder.addResult(beanMethod, GET_CALLER_IDENTITY, callGetCallerIdentity());
        ResultHolder.addResult(beanMethod, IS_CALLER_IN_ROLE,callIsCallerInRole());
        ResultHolder.addResult(beanMethod,IS_CALLER_IN_ROLE_IDENITY,callIsCallerInRole_Identity());
    }

}
