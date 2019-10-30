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

package org.jboss.as.test.integration.jpa.entitylistener;

import javax.annotation.PostConstruct;
import javax.ejb.EJBContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

/**
 * test case from AS7-2968
 */

public class MyListener {


    private static volatile int invocationCount = 0;

    private static volatile int postCtorInvocationCount = 0;

    public static int getInvocationCount() {
        return invocationCount;
    }

    public static void setInvocationCount(int invocationCount) {
        MyListener.invocationCount = invocationCount;
    }

    public static int getPostCtorInvocationCount() {
        return postCtorInvocationCount;
    }

    public static void setPostCtorInvocationCount(int postCtorInvocationCount) {
        MyListener.postCtorInvocationCount = postCtorInvocationCount;
    }

    @PrePersist
    @PreUpdate
    public void onEntityCallback(Object entity) {
        try {
            invocationCount++;
            InitialContext jndiContext = new InitialContext();
            EJBContext ctx = (EJBContext) jndiContext.lookup("java:comp/EJBContext");
            //System.out.println(ctx.getCallerPrincipal().getName() + ", entity=" + entity);
        } catch (NamingException e) {
            throw new RuntimeException("initial context error", e);
        }

    }

    @PostConstruct
    public void postCtor() {
        postCtorInvocationCount++;
    }
}
