/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.group.bean;

import java.io.Serializable;
import java.util.List;

public class ClusterTopology implements Serializable {
    private static final long serialVersionUID = 413628123168918069L;

    private final String targetMember;
    private final List<String> currentMembers;
    private final List<String> previousMembers;

    public ClusterTopology(String targetMember, List<String> currentMembers, List<String> previousMembers) {
        this.targetMember = targetMember;
        this.currentMembers = currentMembers;
        this.previousMembers = previousMembers;
    }

    public String getTargetMember() {
        return this.targetMember;
    }

    public List<String> getCurrentMembers() {
        return this.currentMembers;
    }

    public List<String> getPreviousMembers() {
        return this.previousMembers;
    }
}
