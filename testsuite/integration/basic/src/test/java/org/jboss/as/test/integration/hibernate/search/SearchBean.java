/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.hibernate.search;

import java.util.List;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;

@Stateful
public class SearchBean {

    @PersistenceContext
    EntityManager em;

    public void storeNewBook(String title) {
        Book book = new Book();
        book.title = title;
        em.persist(book);
    }

    public List<Book> findByKeyword(String keyword) {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
        TermQuery termQuery = new TermQuery(new Term("title", keyword));
        FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(termQuery, Book.class);
        return fullTextQuery.getResultList();
    }

    public SearchFactory retrieveHibernateSearchEngine() {
        return Search.getFullTextEntityManager(em).getSearchFactory();
    }

}
