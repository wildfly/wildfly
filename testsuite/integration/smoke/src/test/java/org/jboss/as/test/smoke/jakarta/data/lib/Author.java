/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import java.util.Collections;
import java.util.Set;

import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

@Entity
public class Author {
    @Id
    @GeneratedValue
    private Long id;

    @OneToOne(optional = false)
    private Person person;

    @JsonbTypeSerializer(BooksSerializer.class)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "author")
    private Set<Book> books;

    public Author() {}

    public Author(Person person) {
        this.person = person;
    }

    public Long getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public Set<Book> getBooks() {
        return books != null ? books : Collections.emptySet();
    }

    public void addBook(Book book) {
        books.add(book);
    }
}