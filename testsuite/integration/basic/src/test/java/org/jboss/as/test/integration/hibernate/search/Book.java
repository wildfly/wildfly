package org.jboss.as.test.integration.hibernate.search;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity @Indexed
public class Book {

   @Id @GeneratedValue
   Long id;

   @Field
   String title;

}
