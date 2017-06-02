/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.arjuna;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ee.injection.resource.persistencecontextref.PcMyEntity;
import org.jboss.as.test.integration.ee.injection.resource.persistencecontextref.PcOtherEntity;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Make sure we can run with Arjuna TM.
 * 
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 * @deprecated
 */
@RunWith(Arquillian.class)
public class ArjunaTestCase
{
   private static final Logger log = Logger.getLogger(ArjunaTestCase.class);
   
   private static final String persistence_xml = 
           "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" +  
           "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\">" + 
               "<persistence-unit name=\"test\">" + 
                   "<description>Persistence Unit</description>" + 
                   "<jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" + 
                   "<properties>" +  
                       "<property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\" />" + 
                       "<property name=\"PersistenceUnitName\" value=\"test\" />" + 
                   "</properties>" + 
               "</persistence-unit>" + 
           "</persistence>";

   @Deployment
   public static Archive<?> deploy() {
       JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "arjuna-test.jar");
       jar.addPackage(ArjunaTestCase.class.getPackage());
       // jar.addAsManifestResource("ejb/persistence.xml", "persistence.xml");
       jar.addAsManifestResource(new StringAsset(persistence_xml), "persistence.xml");
       return jar;
   }
   
   @Test
   public void testStatefulTx() throws Exception
   {     
      InitialContext ctx = new InitialContext();
      StatefulTx stateful = (StatefulTx) ctx.lookup("java:module/StatefulTx!org.jboss.as.test.integration.ejb.arjuna.StatefulTx");
      Assert.assertNotNull(stateful);
      
      boolean arjunaTransacted = stateful.isArjunaTransactedRequired();
      Assert.assertTrue(arjunaTransacted);
      arjunaTransacted = stateful.isArjunaTransactedRequiresNew();
      Assert.assertTrue(arjunaTransacted);
      
      Entity entity = new Entity();
      entity.setName("test-entity");
      entity.setId(1234L);
      
      arjunaTransacted = stateful.clear(entity);
      Assert.assertTrue(arjunaTransacted);
      
      arjunaTransacted = stateful.persist(entity);
      Assert.assertTrue(arjunaTransacted);
      
      stateful.clear(entity);
   }
}
