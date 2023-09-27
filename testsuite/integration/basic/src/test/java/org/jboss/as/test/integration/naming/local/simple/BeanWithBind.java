/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.local.simple;

import jakarta.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author John Bailey
 */
@Stateless
public class BeanWithBind {
    public void doBind() {
        try {
            final Context context = new InitialContext();
            context.bind("java:jboss/test", "Test");
            context.bind("java:/test", "Test");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}
