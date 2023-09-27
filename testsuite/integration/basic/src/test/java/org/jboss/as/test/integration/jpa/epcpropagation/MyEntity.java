/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.epcpropagation;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Entity
public class MyEntity implements Serializable {
    private Integer id;
    private String name;

    public MyEntity() {

    }

    public MyEntity(String name) {
        this.name = name;
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
        return "MyEntity:id=" + id + ",name=" + name;
    }
}
