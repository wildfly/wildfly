/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.query.data;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.api.annotations.indexing.Text;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * Adopted and adapted from Infinispan testsuite.
 *
 * @author Radoslav Husar
 * @since 27
 */
@Indexed
public class Book {

    @Text
    @ProtoField(number = 1)
    public final String title;

    @Basic
    @ProtoField(number = 2)
    public final String author;

    @Basic
    @ProtoField(number = 3, defaultValue = "0")
    public final int publicationYear;


    @ProtoFactory
    public Book(String title, String author, int publicationYear) {
        this.title = title;
        this.author = author;
        this.publicationYear = publicationYear;
    }
}
