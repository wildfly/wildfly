/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.query.data;

import org.infinispan.protostream.annotations.ProtoField;

/**
 * Adopted and adapted from Infinispan testsuite.
 *
 * @author Radoslav Husar
 * @since 27
 */
public class Person {

    @ProtoField(1)
    public String name;

    @ProtoField(2)
    public Integer id;

    public Person() {
    }

    public Person(String name) {
        this.name = name;
    }

    public Person(String name, Integer id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Integer getId() {
        return id;
    }
}
