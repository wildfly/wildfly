/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.library;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.as.test.smoke.jakarta.data.lib.Book;
import org.jboss.as.test.smoke.jakarta.data.lib.Librarian;
import org.jboss.as.test.smoke.jakarta.data.lib.Library;
import org.jboss.as.test.smoke.jakarta.data.lib.Person;
import org.jboss.as.test.smoke.jakarta.data.lib.Recruiter;

/** Provides a REST API for manipulating resources associated with a Library. */
@Path("/library")
public class LibraryResource {

    @Inject
    private Recruiter recruiter;

    @Inject
    private LibraryBoard libraryBoard;

    @Path("{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Library getLibrary(@PathParam("name") String name) {
        Optional<Library> optional = libraryBoard.findLibrary(name);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException();
    }

    @Path("{name}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Library openLibrary(@PathParam("name") String name) {
        return libraryBoard.openLibrary(name);
    }

    @Path("{library}/book")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Book> books(@PathParam("library") String library) {
        return getLibrary(library).getBooks();
    }

    @Path("{name}/book/{title}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Book getBook(@PathParam("name") String libraryName, @PathParam("title") String title) {
        Library library = getLibrary(libraryName);
        Optional<Book> book = library.getBook(title);
        if (book.isPresent()) {
            return book.get();
        }
        throw new NotFoundException();
    }

    @Path("{name}/book/{title}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Book addBook(@PathParam("name") String libraryName, @PathParam("title") String title) {
        Library library = getLibrary(libraryName);
        Optional<Book> book = libraryBoard.findBook(title);
        if (book.isPresent()) {
            Book b = book.get();
            library.addBook(b);
            libraryBoard.updateLibrary(library);
            return b;
        }
        throw new BadRequestException("No book titled " + title + " exists");
    }

    @Path("{name}/book/{title}")
    @DELETE
    public void removeBook(@PathParam("name") String libraryName, @PathParam("title") String title) {
        Library library = getLibrary(libraryName);
        Optional<Book> book = libraryBoard.findBook(title);
        book.map(b -> {library.removeBook(b); libraryBoard.updateLibrary(library); return b;}).orElseThrow(() -> new NotFoundException("No book titled " + title + " exists"));
    }

    @Path("{library}/librarian/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Librarian getLibrarian(@PathParam("library") String libraryName, @PathParam("name") String name) {
        Library library = getLibrary(libraryName);
        Optional<Librarian> librarian = library.getLibrarian(name);
        if (librarian.isPresent()) {
            return librarian.get();
        }
        throw new NotFoundException();
    }

    @Path("{library}/librarian/{name}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Librarian hireLibrarian(@PathParam("library") String libraryName, @PathParam("name") String name, @QueryParam("hireDate") String hireDate) {
        Library library = getLibrary(libraryName);
        Optional<Librarian> librarian = library.getLibrarian(name);
        if (librarian.isPresent()) {
            return librarian.get();
        }
        Optional<Person> p = recruiter.find(name);
        return p.map(person -> libraryBoard.hireLibrarian(person, LocalDate.parse(hireDate), library)).orElseThrow(() -> new BadRequestException("No person named " + name + " exists"));
    }

    @Path("{library}/librarian")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Librarian> librarians(@PathParam("library") String library) {
        return getLibrary(library).getLibrarians();
    }

}
