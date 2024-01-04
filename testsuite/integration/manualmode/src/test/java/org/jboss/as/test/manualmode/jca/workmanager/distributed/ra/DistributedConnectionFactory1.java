/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.jca.workmanager.distributed.ra;

import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;
import java.io.Serializable;

public interface DistributedConnectionFactory1 extends Serializable, Referenceable {

    DistributedConnection1 getConnection() throws ResourceException;
}
