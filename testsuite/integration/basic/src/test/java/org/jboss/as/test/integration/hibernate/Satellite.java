/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.hibernate;

/**
 * Represents a satellite object.
 *
 * @author Madhumita Sadhukhan
 */

public class Satellite {

    private Integer id;
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof Satellite)) { return false; }

        final Satellite satellite = (Satellite) o;

        return name.equals(satellite.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
