/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.interceptor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jboss.as.jpa.container.NonTxEmCloser;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.msc.service.ServiceName;

/**
 * Web setup action that closes the entity managers created during the servlet invocation.
 * This provides a thread local collection of all created transactional entity managers (created without a
 * transaction).
 *
 * @author Scott Marlow
 */
public class WebNonTxEmCloserAction implements SetupAction {

    @Override
    public void setup(final Map<String, Object> properties) {
        NonTxEmCloser.pushCall();       // create a thread local place to hold created transactional entity managers
    }

    @Override
    public void teardown(final Map<String, Object> properties) {
        NonTxEmCloser.popCall();    // close any transactional entity managers that were created without a Jakarta Transactions transaction.
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Set<ServiceName> dependencies() {
        return Collections.emptySet();
    }
}
