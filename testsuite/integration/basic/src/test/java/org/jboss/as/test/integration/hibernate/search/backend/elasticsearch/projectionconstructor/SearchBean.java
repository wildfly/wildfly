/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.hibernate.search.backend.elasticsearch.projectionconstructor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.transaction.Transactional;
import org.hibernate.search.mapper.orm.Search;

import java.util.List;

@ApplicationScoped
@Transactional
public class SearchBean {

    @PersistenceContext
    EntityManager em;

    public void storeNewBook(BookDTO bookDTO) {
        Book book = new Book();
        book.setId(bookDTO.id);
        book.setTitle(bookDTO.title);
        for (AuthorDTO authorDTO : bookDTO.authors) {
            Author author = new Author();
            author.setFirstName(authorDTO.firstName);
            author.setLastName(authorDTO.lastName);
            author.getBooks().add(book);
            book.getAuthors().add(author);
            em.persist(author);
        }
        em.persist(book);
    }

    public void deleteAll() {
        CriteriaDelete<Book> deleteBooks = em.getCriteriaBuilder()
                .createCriteriaDelete(Book.class);
        deleteBooks.from(Book.class);
        em.createQuery(deleteBooks).executeUpdate();

        CriteriaDelete<Author> deleteAuthors = em.getCriteriaBuilder()
                .createCriteriaDelete(Author.class);
        deleteAuthors.from(Author.class);
        em.createQuery(deleteAuthors).executeUpdate();

        Search.session(em).workspace(Book.class).purge();
    }

    public List<BookDTO> findByKeyword(String keyword) {
        return Search.session(em).search(Book.class)
                .select(BookDTO.class)
                .where(f -> f.match().field("title").matching(keyword))
                .fetchAllHits();
    }

}
