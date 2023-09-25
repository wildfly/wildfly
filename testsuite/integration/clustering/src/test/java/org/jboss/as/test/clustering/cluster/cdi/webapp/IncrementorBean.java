/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.cdi.webapp;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.Incrementor;

/**
 * @author Tomas Remes
 */
@Named
@SessionScoped
public class IncrementorBean implements Incrementor, Serializable {
    private static final long serialVersionUID = -8232968628862534975L;

    private final AtomicInteger count;

    IncrementorBean() {
        this.count = new AtomicInteger(0);
    }

    @ProtoFactory
    IncrementorBean(int value) {
        this.count = new AtomicInteger(value);
    }

    @Override
    public int increment() {
        return this.count.incrementAndGet();
    }

    @Override
    public void reset() {
        this.count.set(0);
    }

    @ProtoField(number = 1, defaultValue = "0")
    int value() {
        return this.count.get();
    }
}
