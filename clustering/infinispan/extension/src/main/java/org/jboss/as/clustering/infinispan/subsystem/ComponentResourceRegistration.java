/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumerates resource registrations for the components of a cache configuration
 * @author Paul Ferraro
 */
public enum ComponentResourceRegistration implements ResourceRegistration {
    WILDCARD(PathElement.WILDCARD_VALUE),
    BACKUP_SITES("backups"),
    EXPIRATION("expiration"),
    LOCKING("locking"),
    PARTITION_HANDLING("partition-handling"),
    PERSISTENCE("persistence"),
    STATE_TRANSFER("state-transfer"),
    TRANSACTION("transaction"),
    ;
    private final PathElement path;

    ComponentResourceRegistration(String value) {
        this.path = PathElement.pathElement("component", value);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
