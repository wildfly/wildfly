/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.dependencies;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Singleton
@Startup
public class DependentEjb implements StringView {

    private String hello;

    @PostConstruct
    public void post() throws NamingException {
        StringView ejb = (StringView) new InitialContext().lookup("java:global/dependee/DependeeEjb");
        hello = ejb.getString();
    }

    @Override
    public String getString() {
        return hello;
    }
}
