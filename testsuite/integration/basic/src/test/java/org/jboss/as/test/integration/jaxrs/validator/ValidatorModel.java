/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.validator;

import jakarta.validation.constraints.Max;

/**
 * @author Stuart Douglas
 */
public class ValidatorModel {

    @Max(10)
    private int id;

    public ValidatorModel(final int id) {
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
        return "ValidatorModel{" +
                "id=" + id +
                '}';
    }
}
