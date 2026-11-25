/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

@Entity
public class Library {
    @Id
    @GeneratedValue
    private Long id;

    @Basic(optional = false)
    private String name;


    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Book> books;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "library")
    private Set<Librarian> librarians;

    public Library() {

    }

    public Library(String name) {
        this.name = name;
        this.librarians = new HashSet<>();
        this.books = new HashSet<>();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @JsonbTypeSerializer(BooksSerializer.class)
    public Set<Book> getBooks() {
        return books != null ? books : Collections.emptySet();
    }

    public void addBook(Book book) {
        books.add(book);
    }

    public void removeBook(Book book) {
        books.remove(book);
    }

    public Optional<Book> getBook(String title) {
        for (Book book : getBooks()) {
            if (title.equals(book.getTitle())) {
                return Optional.of(book);
            }
        }
        return Optional.empty();
    }
    public Set<Librarian> getLibrarians() {
        return librarians;
    }

    public Optional<Librarian> getLibrarian(String name) {
        return librarians.stream().filter(lib -> name.equals(lib.getPerson().getName())).findFirst();
    }

}
