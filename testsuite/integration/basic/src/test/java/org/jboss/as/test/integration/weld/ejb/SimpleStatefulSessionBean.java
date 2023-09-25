/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb;

import static java.util.concurrent.TimeUnit.SECONDS;
import static jakarta.ejb.TransactionAttributeType.NEVER;

import java.util.concurrent.CountDownLatch;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.AccessTimeout;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;

/**
 * A simple stateful session bean.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateful
@LocalBean
// do not wait, but throw immediately
@AccessTimeout(0)
// we don't want any interference from tx association (probably a bug)
@TransactionAttribute(NEVER)
public class SimpleStatefulSessionBean {
    public String echo(CountDownLatch latch, String msg) throws InterruptedException {
        boolean tripped = latch.await(5, SECONDS);
        if (!tripped)
            return "Timed out";
        return "Echo " + msg;
    }

    @PostConstruct
    public void postConstruct() {
        SimpleServlet.propagated.set("Shared context");
    }
}
