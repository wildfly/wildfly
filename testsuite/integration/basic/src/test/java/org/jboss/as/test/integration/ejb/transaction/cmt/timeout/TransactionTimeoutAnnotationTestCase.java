package org.jboss.as.test.integration.ejb.transaction.cmt.timeout;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import javax.naming.InitialContext;

/**
 */
@RunWith(Arquillian.class)
public class TransactionTimeoutAnnotationTestCase {

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test-ejb-cmt-timeout.jar");
        jar.addClass(BeanWithTimeoutValue.class);
        jar.addClass(TimeoutRemoteView.class);
        jar.addClass(TimeoutLocalView.class);

        return jar;
    }

    /**
     */
    @Test
    public void testBeanTimeouts() throws Exception {
        TimeoutRemoteView remoteView = (TimeoutRemoteView)(new InitialContext().lookup("java:module/BeanWithTimeoutValue!org.jboss.as.test.integration.ejb.transaction.cmt.timeout.TimeoutRemoteView"));
        TimeoutLocalView localView = (TimeoutLocalView)(new InitialContext().lookup("java:module/BeanWithTimeoutValue!org.jboss.as.test.integration.ejb.transaction.cmt.timeout.TimeoutLocalView"));

        long timeoutValue=-1;
        timeoutValue=(long)remoteView.getBeanTimeout();
        Assert.assertEquals("Bean-level timeout failed", 5l, timeoutValue);
        timeoutValue=(long)remoteView.getBeanMethodTimeout();
        Assert.assertEquals("Bean-method timeout failed", 6l, timeoutValue);
        timeoutValue=(long)remoteView.getRemoteMethodTimeout();
        Assert.assertEquals("Remote-method timeout failed", 7l, timeoutValue);
        timeoutValue=(long)localView.getLocalViewTimeout();
        Assert.assertEquals("Local-view timeout failed", 8l, timeoutValue);
    }
}
