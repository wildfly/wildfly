/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inject;

import jakarta.annotation.Resource;
import jakarta.ejb.EJBContext;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import javax.sql.DataSource;

/**
 * SimpleInterceptor
 *
 * @author Jaikiran Pai
 */
public class SimpleInterceptor {

    @Resource(lookup = "java:jboss/datasources/ExampleDS")
    private DataSource ds;

    // an injection-target is configured for this field through ejb-jar.xml
    private EJBContext ejbContextInjectedThroughEjbJarXml;

    // an injection-target is configured for this field through ejb-jar.xml
    private EntityManager persistenceCtxRefConfiguredInEJBJarXml;

    @AroundInvoke
    public Object aroundInvoke(InvocationContext invocationCtx) throws Exception {
        if (ds == null) {
            throw new IllegalStateException("Datasource was *not* injected in interceptor " + this.getClass().getName());
        }

        if (ejbContextInjectedThroughEjbJarXml == null) {
            throw new IllegalStateException("EJBContext was *not* injected in interceptor " + this.getClass().getName());
        }

        if (persistenceCtxRefConfiguredInEJBJarXml == null) {
            throw new IllegalStateException("EntityManager was *not* injected in interceptor " + this.getClass().getName());
        }
        return invocationCtx.proceed();
    }
}
