/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
