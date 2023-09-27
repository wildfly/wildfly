/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.invokedintf;

import jakarta.ejb.Remote;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 */
@Remote
public interface Tester {
    void testAnnotated1() throws TestFailedException;

    void testAnnotated2() throws TestFailedException;

    void testXml1() throws TestFailedException;

    void testXml2() throws TestFailedException;
}
