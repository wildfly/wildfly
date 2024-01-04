/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.services;

import jakarta.jws.WebService;

@WebService(targetNamespace = "http://www.jboss.org/jboss/as/test/TestWS")
public interface TestWS {

    String sayHello();
}
