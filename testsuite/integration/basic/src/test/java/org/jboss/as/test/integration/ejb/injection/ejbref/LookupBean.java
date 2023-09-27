/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejbref;

import jakarta.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;


/**
 * @author Stuart Douglas
 */
@Stateless
public class LookupBean {

    public HomeInterface doLookupRemote() {
        try {
            return (HomeInterface) new InitialContext().lookup("java:comp/env/ejb/remote");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public LocalHomeInterface doLookupLocal() {
        try {
            return (LocalHomeInterface) new InitialContext().lookup("java:comp/env/ejb/local");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}
