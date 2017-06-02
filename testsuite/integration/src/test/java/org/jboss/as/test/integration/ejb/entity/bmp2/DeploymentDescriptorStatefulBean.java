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
import javax.annotation.Resource;
import javax.interceptor.InvocationContext;
import javax.naming.InitialContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

/**
 * @version <tt>$Revision: 67628 $</tt>
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public class DeploymentDescriptorStatefulBean implements DeploymentDescriptorStatefulLocal
{
   @Resource UserTransaction ut;
   
   TransactionManager tm;
   
   @PostConstruct
   public void postConstruct(final InvocationContext context) throws Exception {
       this.tm = (TransactionManager) new InitialContext().lookup("java:jboss/TransactionManager");
   }

   public Transaction beginNoEnd() throws Exception
   {
      if (tm.getTransaction() != null) throw new TestException("THERE IS AN EXISTING TRANSACTION");
      ut.begin();
      return tm.getTransaction();
   }

   public void endCommit(Transaction old) throws Exception
   {
      if (old != tm.getTransaction()) throw new TestException("No matching TX");
      ut.commit();
   }

   public void endRollback(Transaction old) throws Exception
   {
      if (old != tm.getTransaction()) throw new TestException("No matching TX");
      ut.rollback();
   }

   public void beginCommitEnd() throws Exception
   {
      if (tm.getTransaction() != null) throw new TestException("THERE IS AN EXISTING TRANSACTION");
      ut.begin();
      ut.commit();
   }

   public void beginRollbackEnd() throws Exception
   {
      if (tm.getTransaction() != null) throw new TestException("THERE IS AN EXISTING TRANSACTION");
      ut.begin();
      ut.rollback();
   }
   
}
