/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.query.data;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * Adopted and adapted from Infinispan testsuite.
 *
 * @author Radoslav Husar
 * @since 27
 */
@ProtoDoc("@Indexed")
public class Book {

    @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.YES, store = Store.NO)")
    @ProtoField(number = 1)
    public final String title;

    @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.NO, store = Store.NO)")
    @ProtoField(number = 2)
    public final String author;

    @ProtoDoc("@Field(index=Index.YES, store = Store.NO)")
    @ProtoField(number = 3, defaultValue = "0")
    public final int publicationYear;


    @ProtoFactory
    public Book(String title, String author, int publicationYear) {
        this.title = title;
        this.author = author;
        this.publicationYear = publicationYear;
    }
}
