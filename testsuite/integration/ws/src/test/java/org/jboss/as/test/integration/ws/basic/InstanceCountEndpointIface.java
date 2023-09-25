/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.basic;

import jakarta.jws.WebService;

@WebService
public interface InstanceCountEndpointIface {

    int getInstanceCount();
    String test(String payload);
}
