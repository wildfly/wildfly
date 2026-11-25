/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.group.bean;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.GroupMembership;

/**
 * @author Paul Ferraro
 */
public interface Group extends org.wildfly.clustering.server.Group<GroupMember> {
    GroupMembership<GroupMember> getPreviousMembership();
}
