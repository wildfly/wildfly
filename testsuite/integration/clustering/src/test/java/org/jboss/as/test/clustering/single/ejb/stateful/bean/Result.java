/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.stateful.bean;

import java.io.Serial;
import java.io.Serializable;

/**
 * A wrapper for a return value that includes the node on which the result was generated.
 * @author Paul Ferraro
 */
public class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = -1079933234795356933L;

    private final T value;

    public Result(T value) {
        this.value = value;
    }

    public T getValue() {
        return this.value;
    }
}
