/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.remote.ejb;

import jakarta.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author John Bailey
 */
@Stateless
public class Singleton implements BinderRemote {
    public String echo(String value) {
        return "Echo: " + value;
    }

    private final String JNDI_NAME = "java:jboss/exported/some/entry";

    // methods to do JNDI binding for remote access
    public void bind() {
        try {
            InitialContext ic = new InitialContext();
            ic.bind(JNDI_NAME, "Test");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public void rebind() {
        try {
            InitialContext ic = new InitialContext();
            ic.rebind(JNDI_NAME, "Test2");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public void unbind() {
        try {
            InitialContext ic = new InitialContext();
            ic.unbind(JNDI_NAME);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}
