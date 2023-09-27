/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.servlet;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface Session30 {
    void hello();

    void goodbye();

    String access(TestObject o);

    TestObject createTestObject();

    boolean checkEqPointer(TestObject to);

    WarTestObject getWarTestObject();
}
