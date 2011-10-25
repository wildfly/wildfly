/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.entity.bmp2;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.InvocationContext;
import javax.naming.InitialContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.jboss.logging.Logger;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 67628 $
 */
@Stateless
public class TesterBean implements TesterRemote
{   
   @EJB StatelessLocal stateless;
   
   TransactionManager tm;
   
   @PostConstruct
   public void postConstruct(final InvocationContext context) throws Exception {
       this.tm = (TransactionManager) new InitialContext().lookup("java:jboss/TransactionManager");
   }


   protected static Logger log = Logger.getLogger(TesterBean.class);

   public void testStatelessWithTx() throws Exception
   {
      stateless.beginCommitEnd();
      stateless.beginRollbackEnd();

      try
      {
         stateless.beginNoEnd();
      }
      catch (Exception e)
      {
         if (e instanceof TestException) throw e;
         log.info("should be EJBException thrown that begin and no end was called: ", e);
      }
   }

   @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
   public void testStatelessWithoutTx() throws Exception
   {
      stateless.beginCommitEnd();
      if (tm.getTransaction() != null) throw new RuntimeException("tx is associated");
      stateless.beginRollbackEnd();
      if (tm.getTransaction() != null) throw new RuntimeException("tx is associated");
      
      try
      {
         stateless.beginNoEnd();
      }
      catch (Exception e)
      {
         if (e instanceof TestException) throw e;
         log.info("should be EJBException thrown that begin and no end was called: ", e);
      }
      if (tm.getTransaction() != null) throw new RuntimeException("tx is associated");
   }

   StatefulLocal getStateful() throws Exception
   {
      InitialContext ctx = new InitialContext();
      return (StatefulLocal)ctx.lookup("java:module/StatefulBean!org.jboss.as.test.integration.ejb.entity.bmp2.StatefulLocal");
   }

   public void testStatefulWithTx() throws Exception
   {
      StatefulLocal stateful = getStateful();
      Transaction tx = stateful.beginNoEnd();
      stateful.endCommit(tx);
      tx = stateful.beginNoEnd();
      stateful.endRollback(tx);

      stateful.beginCommitEnd();
      stateful.beginRollbackEnd();

   }

   @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
   public void testStatefulWithoutTx() throws Exception
   {
      StatefulLocal stateful = getStateful();
      Transaction tx = stateful.beginNoEnd();
      if (tm.getTransaction() != null) throw new RuntimeException("tx is associated");
      stateful.endCommit(tx);
      if (tm.getTransaction() != null) throw new RuntimeException("tx is associated");
      tx = stateful.beginNoEnd();
      if (tm.getTransaction() != null) throw new RuntimeException("tx is associated");
      stateful.endRollback(tx);
      if (tm.getTransaction() != null) throw new RuntimeException("tx is associated");

      stateful.beginCommitEnd();
      if (tm.getTransaction() != null) throw new RuntimeException("tx is associated");
      stateful.beginRollbackEnd();
      if (tm.getTransaction() != null) throw new RuntimeException("tx is associated");

   }
}
