/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hibernate.envers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

/**
 * @author Madhumita Sadhukhan
 */
@Entity
@Audited
public class PhoneMO {
    @Id
    @GeneratedValue
    @Column(name = "PHONE_ID")
    private Integer id;

    private String type;

    private String number;

    private String areacode;

    @ManyToOne
    @JoinTable(name = "CUSTOMER_PHONE", joinColumns = {@JoinColumn(name = "PHONE_ID", referencedColumnName = "PHONE_ID")}, inverseJoinColumns = {@JoinColumn(name = "CUST_ID", referencedColumnName = "CUST_ID")})
    @AuditJoinTable(name = "CUSTOMER_PHONE_AUD", inverseJoinColumns = {@JoinColumn(name = "CUST_ID", referencedColumnName = "CUST_ID")})
    private CustomerMO customer;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAreacode() {
        return areacode;
    }

    public void setAreacode(String areacode) {
        this.areacode = areacode;
    }

    public CustomerMO getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerMO customer) {
        this.customer = customer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof PhoneMO)) { return false; }

        final PhoneMO phone = (PhoneMO) o;

        return id != null ? id.equals(phone.id) : phone.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

}
