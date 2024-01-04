/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hibernate.entity;

import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

/**
 * Flight entity class
 *
 * @author Zbyněk Roubalík
 */
@Entity
public class Flight {

    private Long id;
    private String name;
    private Company company;
    private Set<Customer> customers;

    public Flight() {
    }

    @Id
    @Column(name = "flight_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long long1) {
        id = long1;
    }

    @Column(updatable = false, name = "flight_name", nullable = false, length = 50)
    public String getName() {
        return name;
    }

    public void setName(String string) {
        name = string;
    }


    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "comp_id")
    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    public Set<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(Set<Customer> customers) {
        this.customers = customers;
    }

}
