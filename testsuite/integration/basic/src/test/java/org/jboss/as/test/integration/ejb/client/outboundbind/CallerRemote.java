/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.client.outboundbind;

import jakarta.ejb.Remote;

@Remote
public interface CallerRemote {
    String callAndGetSourceAddress() throws Exception;
}
