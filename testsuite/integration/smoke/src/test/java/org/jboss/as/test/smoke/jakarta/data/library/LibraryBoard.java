/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.library;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;
import org.jboss.as.test.smoke.jakarta.data.lib.Book;
import org.jboss.as.test.smoke.jakarta.data.lib.Librarian;
import org.jboss.as.test.smoke.jakarta.data.lib.Library;
import org.jboss.as.test.smoke.jakarta.data.lib.Person;

/** Repository for manipulating entities managed by the board of a library system. */
@Repository
public interface LibraryBoard {

    default Library openLibrary(String name) {
        return openLibrary(new Library(name));
    }
    @Insert
    Library openLibrary(Library library);

    @Update
    void updateLibrary(Library library);

    @Delete
    void closeLibrary(Library library);

    @Find
    Optional<Library> findLibrary(String name);

    default Librarian hireLibrarian(Person person, LocalDate hireDate, Library library) {
        return hireLibrarian(new Librarian(person, hireDate, library));
    }

    @Insert
    Librarian hireLibrarian(Librarian librarian);

    @Delete
    void fireLibrarian(Librarian librarian);

    @Find
    Optional<Book> findBook(String title);

    @Query("where author.person.name like :name")
    List<Book> findBooksByAuthorName(String name);
}
