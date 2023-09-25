/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.web.annotationsmodule;

import jakarta.ejb.Stateless;

@Stateless
public class TestEjb {

    public static final String TEST_EJB = "TestEjb";

    public String hello() {
        return TEST_EJB;
    }
}
