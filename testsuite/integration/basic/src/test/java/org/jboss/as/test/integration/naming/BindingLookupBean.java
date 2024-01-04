/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.naming;

import jakarta.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author Eduardo Martins
 */
@Stateless
public class BindingLookupBean {

    public Object lookupBind(String name) throws NamingException {
        return new InitialContext().lookup(name);
    }

}
