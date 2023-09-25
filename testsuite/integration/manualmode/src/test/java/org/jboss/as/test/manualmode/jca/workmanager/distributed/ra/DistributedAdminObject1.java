/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.jca.workmanager.distributed.ra;

import jakarta.resource.Referenceable;
import java.io.Serializable;

/**
 * DistributedAdminObject1 allows the tester to use the resource adapter and workmanager from
 * outside EAP.
 */
public interface DistributedAdminObject1 extends Referenceable, Serializable {

    void setName(String name);

    String getName();
}
