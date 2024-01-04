/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.hibernate.search.backend.elasticsearch.simple;

import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.mapper.orm.Search;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
@Transactional
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
        Search.session(em).workspace(Book.class).purge();
    }

    public List<Book> findByKeyword(String keyword) {
        return Search.session(em).search(Book.class)
                .where(f -> f.match().field("title").matching(keyword))
                .fetchAllHits();
    }

    public List<Book> findAutocomplete(String term) {
        return Search.session(em).search(Book.class)
                .where(f -> f.simpleQueryString().field("title_autocomplete")
                        .matching(term)
                        .defaultOperator(BooleanOperator.AND))
                .fetchAllHits();
    }

}
