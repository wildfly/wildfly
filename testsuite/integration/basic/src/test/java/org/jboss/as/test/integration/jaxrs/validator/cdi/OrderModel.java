/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.validator.cdi;

/**
 * An order.
 *
 * @author Gunnar Morling
 */
public class OrderModel {

    private int id;

    public OrderModel(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "OrderModel{" + "id=" + id + "}";
    }
}
