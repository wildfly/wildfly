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
package org.jboss.as.test.integration.hibernate.search.backend.lucene.simple;

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
