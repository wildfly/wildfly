/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.dependencies;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Singleton
@Startup
public class DependeeEjb implements StringView {

    public String hello;

    @PostConstruct
    public void post() throws InterruptedException {
        Thread.sleep(100);
        hello = "hello";
    }

    @Override
    public String getString() {
        return hello;
    }
}
