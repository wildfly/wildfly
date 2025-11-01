/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.publisher;

import java.util.List;
import java.util.Optional;

import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;
import org.jboss.as.test.smoke.jakarta.data.lib.Author;
import org.jboss.as.test.smoke.jakarta.data.lib.Book;
import org.jboss.as.test.smoke.jakarta.data.lib.Person;

/** Repository for manipulating entities managed by a publisher. */
@Repository
public interface Publisher {

    default Author signAuthor(Person person) {
        return signAuthor(new Author(person));
    }

    @Insert
    Author signAuthor(Author author);

    @Query("select author from Author author inner join author.person person where person.name = :name")
    Optional<Author> findAuthorByName(String name);

    @Update
    Author updateAuthor(Author author);

    default Book publish(String title, int pageCount, Author author) {
        Book book = new Book(title, author);
        book.setPageCount(pageCount);
        book = publish(book);
        // Ensure the books are loaded
        book.getAuthor().addBook(book);
        return book;
    }

    @Insert
    Book publish(Book newBook);

    @Find
    Optional<Book> findBookByTitle(String title);

    @Find
    @OrderBy("pageCount")
    List<Book> booksByPageCount();
}
