/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.publisher;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.as.test.smoke.jakarta.data.lib.Author;
import org.jboss.as.test.smoke.jakarta.data.lib.Book;
import org.jboss.as.test.smoke.jakarta.data.lib.Person;
import org.jboss.as.test.smoke.jakarta.data.lib.Recruiter;

/** Provides a REST API for manipulating Author resources. */
@Path("/author")
public class AuthorResource {

    @Inject
    private Recruiter recruiter;
    @Inject
    private Publisher publisher;

    @Path("{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Author get(@PathParam("name") String name) {
        Optional<Author> optional = publisher.findAuthorByName(name);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException();
    }

    @Path("{name}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Author signAuthor(@PathParam("name") String name) {
        Optional<Person> p = recruiter.find(name);
        return publisher.signAuthor(p.orElseThrow(() -> new IllegalArgumentException(name)));
    }

    @Path("{name}/books")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Book> books(@PathParam("name") String name) {
        return get(name).getBooks();
    }

}
