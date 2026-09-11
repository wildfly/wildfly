/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.ejb.outboundbind;

import jakarta.ejb.Remote;

@Remote
public interface SourceAddressRemote {
    String getCallerSourceAddress();
}
