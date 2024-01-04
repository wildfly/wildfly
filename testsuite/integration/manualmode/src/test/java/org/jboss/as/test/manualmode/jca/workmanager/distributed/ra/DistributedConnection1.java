/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.jca.workmanager.distributed.ra;

public interface DistributedConnection1 {

    String test(String s);

    void close();
}
