/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hibernate.envers;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

/**
 * @author Madhumita Sadhukhan
 */
@Entity
@Audited
public class Customer {
    @Id
    @GeneratedValue
    @Column(name = "CUST_ID")
    private Integer id;

    private String firstname;

    private String surname;

    @OneToMany
    @JoinTable(name = "CUSTOMER_PHONE", joinColumns = {@JoinColumn(name = "CUST_ID", referencedColumnName = "CUST_ID")}, inverseJoinColumns = {@JoinColumn(name = "PHONE_ID", referencedColumnName = "PHONE_ID")})
    @AuditJoinTable(name = "CUSTOMER_PHONE_AUD", inverseJoinColumns = {@JoinColumn(name = "PHONE_ID", referencedColumnName = "PHONE_ID")})
    private List<Phone> phones = new ArrayList<Phone>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public List<Phone> getPhones() {
        return phones;
    }

    public void setSurname(List<Phone> phones) {
        this.phones = phones;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof Customer)) { return false; }

        final Customer cust = (Customer) o;

        return id != null ? id.equals(cust.id) : cust.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

}
