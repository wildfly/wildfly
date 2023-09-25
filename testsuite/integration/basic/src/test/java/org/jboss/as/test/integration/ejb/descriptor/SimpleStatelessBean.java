/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.descriptor;

import jakarta.annotation.Resource;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.interceptor.InvocationContext;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless
@LocalBean
public class SimpleStatelessBean {
    @Resource(name="test")
    private String test;

    // added in ejb-jar.xml
    public Object aroundInvoke(final InvocationContext context) throws Exception {
        return "*" + context.proceed();
    }

    public String getTest() {
        return test;
    }
}
