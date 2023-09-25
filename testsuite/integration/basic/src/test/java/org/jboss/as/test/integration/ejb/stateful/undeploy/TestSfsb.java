/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.stateful.undeploy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Stateful;

/**
 * @author Stuart Douglas
 */
@Stateful
public class TestSfsb implements TestSfsbRemote{

    @PostConstruct
    public void init() {
        TestServlet.addMessage("PostConstruct");
    }

    @PreDestroy
    public void destroy() {
        TestServlet.addMessage("PreDestroy");
    }

    @Override
    public void invoke() {
        TestServlet.addMessage("invoke");
    }
}
