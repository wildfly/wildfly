/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.ejb.http;

public interface StatelessLocal extends jakarta.ejb.EJBLocalObject {
    int method() throws Exception;
}
