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
package org.jboss.as.testsuite.integration.tm.bmtcleanup;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;


/**
 * BMTCleanUpBean.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author istudens@redhat.com
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class BMTCleanUpBean
{
    @Resource
    private EJBContext ejbContext;

    public void doNormal()
    {
        UserTransaction ut = ejbContext.getUserTransaction();
        try
        {
            ut.begin();
            ut.commit();
        }
        catch (Exception e)
        {
            throw new EJBException("Error", e);
        }
    }

    public void testIncomplete()
    {
        BMTCleanUpBean remote = getBean();
        try
        {
            remote.doIncomplete();
        }
        catch (EJBException expected)
        {
            // expected
        }
        checkTransaction();
        remote.doNormal();
    }

    public void doIncomplete()
    {
        UserTransaction ut = ejbContext.getUserTransaction();
        try
        {
            ut.begin();
        }
        catch (Exception e)
        {
            throw new EJBException("Error", e);
        }
    }

    public void testTxTimeout()
    {
        BMTCleanUpBean remote = getBean();
        try
        {
            remote.doTimeout();
        }
        catch (EJBException expected)
        {
            // expected
        }
        checkTransaction();
        remote.doNormal();
    }

    public void doTimeout()
    {
        UserTransaction ut = ejbContext.getUserTransaction();
        try
        {
            ut.setTransactionTimeout(5);
            ut.begin();
            Thread.sleep(10000);
        }
        catch (InterruptedException ignored)
        {
        }
        catch (Exception e)
        {
            throw new EJBException("Error", e);
        }
    }

    private void checkTransaction()
    {
        try
        {
            TransactionManager tm = (TransactionManager) new InitialContext().lookup("java:jboss/TransactionManager");
            Transaction tx = tm.getTransaction();
            if (tx != null)
                throw new EJBException("There should be no transaction context: " + tx);
        }
        catch (EJBException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new EJBException("Error", e);
        }
    }

    private BMTCleanUpBean getBean()
    {
        try
        {
            // java:global/bmtcleanuptest/BMTCleanUpBean
            return (BMTCleanUpBean) new InitialContext().lookup("java:global/" + BMTCleanUpUnitTestCase.ARCHIVE_NAME + "/" + BMTCleanUpBean.class.getSimpleName());
        }
        catch (NamingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
