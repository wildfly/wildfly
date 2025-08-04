/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.preview.persistence.cdi;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Employee entity class
 *
 * @author Scott Marlow
 */
@Entity
public class Employee {
    @Id
    private int id;

    private String name;

    private String address;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getId() {

        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
