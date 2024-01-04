/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb2.remote.bean;

import java.io.Serializable;

/**
 * A wrapper for a return value that includes the node on which the result was generated.
 * @author Paul Ferraro
 */
public class Result<T> implements Serializable {
    private static final long serialVersionUID = -1079933234795356933L;

    private final T value;
    private final String node;

    public Result(T value) {
        this.value = value;
        this.node = System.getProperty("jboss.node.name");
    }

    public T getValue() {
        return this.value;
    }

    public String getNode() {
        return this.node;
    }
}
