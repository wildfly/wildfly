/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.assertEquals;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verify deployed applications can use the default Hibernate Search module via JPA APIs.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 */
@RunWith(Arquillian.class)
public class HibernateSearchJPATestCase {

   private static final String ARCHIVE_NAME = "hibernate4native_search_test";

   private static final String persistence_xml =
         "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
         "<persistence xmlns=\"http://xmlns.jcp.org/xml/ns/persistence\" version=\"2.1\">" +
         "   <persistence-unit name=\"jpa-search-test-pu\">" +
         "      <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
         "      <properties>" +
         "         <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\" />" +
         "         <property name=\"hibernate.search.default.directory_provider\" value=\"infinispan\" />" +
         "         <property name=\"hibernate.search.infinispan.configuration_resourcename\" value=\"hsearch-infinispan-local.xml\" />" +
         "      </properties>" +
         "   </persistence-unit>" +
         "</persistence>";

   private static final String infinispan_configuration =
         "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
         "<infinispan" +
         "   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
         "   xsi:schemaLocation=\"urn:infinispan:config:6.0 http://www.infinispan.org/schemas/infinispan-config-6.0.xsd\"" +
         "   xmlns=\"urn:infinispan:config:6.0\"> " +
         "   <global>" +
         "      <globalJmxStatistics enabled=\"false\" />" +
         // <!-- Don't enable a JGroups Transport to speedup testing -->
         // (Which is why we need a custom configuration file: the default starts a UDP based channel to do group discovery)
         "      <shutdown hookBehavior=\"DONT_REGISTER\" />" +
         "   </global>" +
         "   <default>" +
         "   </default>" +
         "   <namedCache name=\"LuceneIndexesMetadata\" />" +
         "   <namedCache name=\"LuceneIndexesData\" />" +
         "   <namedCache name=\"LuceneIndexesLocking\" />" +
         "</infinispan>";

   @EJB(mappedName = "java:module/SearchBean")
   private SearchBean searchBean;

   @Test
   public void testFullTextQuery() {
      searchBean.storeNewBook("Hello");
      searchBean.storeNewBook("Hello world");
      searchBean.storeNewBook("Hello planet Mars");
      assertEquals(3, searchBean.findByKeyword("hello").size());
      assertEquals(1, searchBean.findByKeyword("mars").size());
   }

   @Deployment
   public static Archive<?> deploy() throws Exception {

      JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
      // add required dependencies
      jar.addAsManifestResource(
            new StringAsset("Dependencies: org.hibernate.search.orm services\n"), "MANIFEST.MF");
      // add JPA configuration
      jar.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
      jar.addAsResource(new StringAsset(infinispan_configuration), "hsearch-infinispan-local.xml");
      // add testing Bean and entities
      jar.addClasses(SearchBean.class, Book.class, HibernateSearchJPATestCase.class);

      return jar;
   }

}
