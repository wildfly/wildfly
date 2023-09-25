/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.infinispan.bean;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author Paul Ferraro
 */
public class Key {

    private final String key;

    @ProtoFactory
    public Key(String key) {
        this.key = key;
    }

    @ProtoField(number = 1)
    public String getKey() {
        return this.key;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Key)) return false;
        return this.key.equals(object.toString());
    }

    @Override
    public String toString() {
        return this.key;
    }
}
