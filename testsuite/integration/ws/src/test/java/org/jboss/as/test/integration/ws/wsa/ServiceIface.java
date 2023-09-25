/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsa;

import jakarta.jws.WebService;

@WebService(targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/wsaddressing")
public interface ServiceIface {

    String sayHello(String name);
}
