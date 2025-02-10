/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hibernate;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * The entity class will not be enhanced since the property accessors (get/set/is methods) have Jakarta Persistence annotations.
 * This covers https://hibernate.atlassian.net/browse/HHH-16572 determination that EmployeeWithLastNameNotEnhanced should not be enhanced.
 * The reason being that the property accessor also has a jakarta.persistence annnotation (note that jakarta.persistence.Transient
 * would be ignored) that cannot currently be enhanced.
 *
 * For reference see org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl#checkUnsupportedAttributeNaming which will eventually
 * be removed in a future Hibernate ORM (minor or major) release.  When EnhancerImpl#checkUnsupportedAttributeNaming is removed, this test will
 * likely fail and either can be changed or removed at that time.
 *
 * It is worth pointing out that this class is badly written though as the property accessor method name(s) should match the field names.
 * For example, instead of getLastName() + setLastName(), the method name should match the field name, so better would be getName() + setName().
 *
 * @author Scott Marlow
 */
@Entity
@Cacheable(true)
public class EmployeeWithLastNameNotEnhanced {
    @Id
    Long id;

    private String last;

    private String address;

    public String getLastName() {
        return last;
    }

    public void setLastName(String name) {
        this.last = name;
    }

    @Basic
    @Access(AccessType.PROPERTY)
    public String getPropertyMethod() {
        return "from getter: " + last;
    }
    public void setPropertyMethod(String property) {
        this.last = property;
    }
    public String getAddress() {
    return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


}
