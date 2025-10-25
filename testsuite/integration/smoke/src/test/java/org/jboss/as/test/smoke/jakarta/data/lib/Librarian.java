/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import java.time.LocalDate;

import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class Librarian {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    private Person person;

    @Basic(optional = false)
    private LocalDate hireDate;

    @ManyToOne
    @JoinColumn(nullable=false)
    private Library library;

    public Librarian() {}

    public Librarian(Person person, LocalDate hireDate, Library library) {
        this.person = person;
        this.hireDate = hireDate;
        this.library = library;
    }

    public Long getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    @JsonbTypeSerializer(LibrarySerializer.class)
    public Library getLibrary() {
        return library;
    }
}
