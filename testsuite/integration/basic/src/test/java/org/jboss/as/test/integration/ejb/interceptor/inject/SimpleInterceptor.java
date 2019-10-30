/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.interceptor.inject;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
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
