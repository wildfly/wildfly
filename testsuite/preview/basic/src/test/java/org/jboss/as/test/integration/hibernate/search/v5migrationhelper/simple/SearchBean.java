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
package org.jboss.as.test.integration.hibernate.search.v5migrationhelper.simple;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaDelete;
import java.util.List;

@Stateful
// We know the migration helper is deprecated; we want to test it anyway.
@SuppressWarnings("deprecation")
public class SearchBean {

    @PersistenceContext
    EntityManager em;

    public void storeNewBook(String title) {
        Book book = new Book();
        book.title = title;
        em.persist(book);
    }

    public void deleteAll() {
        CriteriaDelete<Book> delete = em.getCriteriaBuilder()
                .createCriteriaDelete(Book.class);
        delete.from(Book.class);
        em.createQuery(delete).executeUpdate();
        Search.getFullTextEntityManager(em).purgeAll(Book.class);
    }

    @SuppressWarnings("unchecked")
    public List<Book> findByKeyword(String keyword) {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
        QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
        Query query = qb.keyword().onField("title").matching(keyword).createQuery();
        FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(query, Book.class);
        return fullTextQuery.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Book> findAutocomplete(String term) {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
        QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class)
                .overridesForField("title_autocomplete", AnalysisConfigurer.AUTOCOMPLETE_QUERY)
                .get();
        Query query = qb.simpleQueryString().onField("title_autocomplete")
                .withAndAsDefaultOperator().matching(term).createQuery();
        FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(query, Book.class);
        return fullTextQuery.getResultList();
    }


}
