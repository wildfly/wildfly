/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.preview.mvc;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named
@RequestScoped
public class CdiBean {
    private String name;

    public String getFirstName() {
        return name;
    }

    public void setFirstName(String name) {
        this.name = name;
    }
}
