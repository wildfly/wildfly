/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import jakarta.persistence.SynchronizationType;

/**
 * SynchronizationTypeAccess provides access to the SynchronizationType for an EntityManager.
 *
 * @author Scott Marlow
 */
public interface SynchronizationTypeAccess {

    SynchronizationType getSynchronizationType();
}
