/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.epcpropagation.hierarchy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Stuart Douglas
 */
@Entity
public class Bus {
    private Integer id;
    private String name;

    public Bus() {

    }

    public Bus(int id, String name) {
        this.name = name;
        this.id = id;
    }

    @Id
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

    public String toString() {
        return "Bus:id=" + id + ",name=" + name;
    }

}
