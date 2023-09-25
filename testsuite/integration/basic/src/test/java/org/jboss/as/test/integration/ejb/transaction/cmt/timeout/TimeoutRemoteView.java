/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.cmt.timeout;

import jakarta.ejb.Remote;

@Remote
public interface TimeoutRemoteView {

    int getBeanTimeout();

    int getBeanMethodTimeout();

    int getRemoteMethodTimeout();
}
