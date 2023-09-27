/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.remote.simple;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.naming.InitialContext;

/**
 * @author John Bailey
 */
@Singleton
@Startup
public class BindingEjb {

    @PostConstruct
    public void bind() throws Exception {

        new InitialContext().bind("java:jboss/exported/test", "TestValue");
        new InitialContext().bind("java:jboss/exported/context/test", "TestValue");

    }
}
