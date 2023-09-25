/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.initializeinorder;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJBContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

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
