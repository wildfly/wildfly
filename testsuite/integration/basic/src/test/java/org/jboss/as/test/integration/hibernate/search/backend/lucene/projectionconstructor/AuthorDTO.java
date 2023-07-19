/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.hibernate.search.backend.lucene.projectionconstructor;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

import java.util.Collections;
import java.util.List;

public class AuthorDTO {
    public final String firstName;
    public final String lastName;
    public final List<BookDTO> books;

    public AuthorDTO(String firstName, String lastName) {
        this(firstName, lastName, Collections.emptyList());
    }

    @ProjectionConstructor
    public AuthorDTO(// These @FieldProjection annotations and @ObjectProjection.path wouldn't be necessary
                     // with a record or a class compiled with -parameters
                     @FieldProjection(path = "firstName") String firstName,
                     @FieldProjection(path = "lastName") String lastName,
                     @ObjectProjection(path = "books", includeDepth = 1) List<BookDTO> books) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.books = books;
    }
}
