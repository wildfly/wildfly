/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.common;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class Naming {
    public static <T> T lookup(final String name, final Class<T> cls) throws NamingException {
        InitialContext ctx = new InitialContext();
        try {
           return cls.cast(ctx.lookup(name));
        }
        finally {
           ctx.close();
        }
    }
}
