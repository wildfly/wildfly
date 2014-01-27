package org.jboss.as.test.integration.hibernate.search;

import java.util.List;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
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

}
