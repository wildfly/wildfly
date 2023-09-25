/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.shared;

import java.io.Serializable;

/**
 * @author Jaikiran Pai
 */
public class CounterResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private int count;
    private String nodeName;

    public CounterResult(final int count, final String nodeName) {
        this.count = count;
        this.nodeName = nodeName;
    }

    public int getCount() {
        return this.count;
    }

    public String getNodeName() {
        return this.nodeName;
    }
}
