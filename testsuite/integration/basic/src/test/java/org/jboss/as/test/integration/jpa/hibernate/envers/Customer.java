/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.jpa.hibernate.envers;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;

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
