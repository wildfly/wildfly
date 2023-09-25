/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.persistencecontextref;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Entity
public class PcMyEntity implements Serializable {
    private Integer id;
    private String name;

    public PcMyEntity() {

    }

    public PcMyEntity(String name) {
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
