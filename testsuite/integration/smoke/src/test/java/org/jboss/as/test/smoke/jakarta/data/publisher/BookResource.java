/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.publisher;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.as.test.smoke.jakarta.data.lib.Author;
import org.jboss.as.test.smoke.jakarta.data.lib.Book;

/** Provides a REST API for manipulating Book resources. */
@Path("/book")
public class BookResource {
    @Inject
    private Publisher publisher;

    @Path("{title}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Book get(@PathParam("title") String title) {
        Optional<Book> optional = publisher.findBookByTitle(title);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException();
    }

    @Path("{title}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Book publish(@PathParam("title") String title, @QueryParam("pageCount") int pageCount, @QueryParam("authorName") String authorName) {
        Optional<Author> author = publisher.findAuthorByName(authorName);
        if (author.isEmpty()) {
            throw new BadRequestException("No author named " + authorName + " is signed");
        }
        return publisher.publish(title, pageCount, author.get());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Book> getBooks() {
        return publisher.booksByPageCount();
    }

}
