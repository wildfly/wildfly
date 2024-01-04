/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.callerprincipal;

public interface IBeanLifecycleCallback {
    String get();
    void remove();
}
