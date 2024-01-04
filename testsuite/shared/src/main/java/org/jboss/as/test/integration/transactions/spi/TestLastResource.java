/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.transactions.spi;

import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.tm.LastResource;

/**
 * Test {@link LastResource} class which causes that <code>XAOnePhaseResource</code>
 * will be instantiated at <code>TransactionImple#createRecord</code>.<br>
 * The information about {@link LastResource} is taken from definition
 * <code>jtaEnvironmentBean.setLastResourceOptimisationInterfaceClassName</code>
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
public class TestLastResource extends TestXAResource implements LastResource {

    public TestLastResource(TransactionCheckerSingleton checker) {
        super(checker);
    }

    public TestLastResource(TestAction testAction, TransactionCheckerSingleton checker) {
        super(testAction, checker);
    }

}
