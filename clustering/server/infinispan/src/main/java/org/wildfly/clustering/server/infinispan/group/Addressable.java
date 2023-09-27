/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import org.jgroups.Address;

/**
 * A object that is identified by a JGroups {@link Address}.
 * @author Paul Ferraro
 */
public interface Addressable {
    Address getAddress();
}
