package org.jboss.as.test.integration.ejb.transaction.annotation;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class AnnotatedTransactionTestCase {

    @ArquillianResource
    private InitialContext initialContext;

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "bz-1466909.jar");
        jar.addPackage(AnnotatedTransactionTestCase.class.getPackage());

        return jar;
    }

    @Test
    public void testMethodHasTransaction() throws SystemException, NotSupportedException, NamingException {
        final UserTransaction userTransaction = (UserTransaction) new InitialContext().lookup("java:jboss/UserTransaction");
        final AnnotatedTx bean = (AnnotatedTx) initialContext.lookup("java:module/" + AnnotatedTxBean.class.getSimpleName() + "!" + AnnotatedTx.class.getName());
        userTransaction.begin();
        try {
            Assert.assertEquals(Status.STATUS_ACTIVE, bean.getActiveTransaction());
        } finally {
            userTransaction.rollback();
        }
    }

    @Test
    public void testMethodHasNoTransaction() throws SystemException, NotSupportedException, NamingException {
        final UserTransaction userTransaction = (UserTransaction) new InitialContext().lookup("java:jboss/UserTransaction");
        final AnnotatedTx bean = (AnnotatedTx) initialContext.lookup("java:module/" + AnnotatedTxBean.class.getSimpleName() + "!" + AnnotatedTx.class.getName());

        bean.getNonActiveTransaction();
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, bean.getNonActiveTransaction());

        try {
            userTransaction.begin();
            bean.getNonActiveTransaction();
            Assert.fail();
        } catch (EJBException e) {
            Assert.assertTrue(true);
        } finally {
            userTransaction.rollback();
        }
    }
}
