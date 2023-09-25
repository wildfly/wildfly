/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.persistenceunitref;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Entity
public class PuOtherEntity implements Serializable {
    private Integer id;
    private String name;

    public PuOtherEntity() {

    }

    public PuOtherEntity(String name) {
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
        return "OtherEntity:id=" + id + ",name=" + name;
    }
}
