/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.group.bean.legacy;

import org.wildfly.clustering.group.Membership;

/**
 * @author Paul Ferraro
 */
public interface LegacyGroup extends org.wildfly.clustering.group.Group {
    Membership getPreviousMembership();
}
