/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hibernate;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Ensure that this entity class is enhanced even though is has property accessor (lastName) referencing field (last)
 * that doesn't match the property name (lastName).
 * The entity class will be enhanced since the property accessors (get/set/is methods) does not use any Jakarta Persistence annotations.
 * This covers the https://hibernate.atlassian.net/browse/HHH-16572 added logic that approves EmployeeWithLastName to be enhanced.
 * For reference see org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl#checkUnsupportedAttributeNaming
 *
 * It is worth pointing out that this class is badly written though as the property accessor method name(s) should match the field names.
 * For example, instead of getLastName() + setLastName(), the method name should match the field name, so better would be getName() + setName().
 *
 * @author Scott Marlow
 */
@Entity
@Cacheable(true)
public class EmployeeWithLastName {

    @Id
    @Column
    private String id;
    public String getId() {
        return id;
    }

    public void setId(String value) {
        id = value;
    }


    private String last;

    private String address;

    public String getLastName() {
        return last;
    }

    public void setLastName(String name) {
        this.last = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


}
