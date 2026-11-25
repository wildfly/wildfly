/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.jsonb;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Employee entity class
 *
 * @author Scott Marlow
 */
@Entity
public class Employee {
    @Id
    private int id;

    @Version
    private Integer version;


    public List<String> getJsonValue() {
        return jsonValue;
    }

    public void setJsonValue(List<String> value) {
        this.jsonValue = value;
    }

    @JdbcTypeCode(SqlTypes.JSON)
    public List<String> jsonValue;

    private String name;

    private String address;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

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
