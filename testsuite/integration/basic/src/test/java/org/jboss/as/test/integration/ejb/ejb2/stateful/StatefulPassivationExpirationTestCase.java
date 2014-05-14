package org.jboss.as.test.integration.ejb.ejb2.stateful;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.CreateException;
import javax.ejb.RemoveException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import javax.transaction.TransactionRolledbackException;

import static junit.framework.Assert.assertTrue;

/**
 * The test creates 500 stateful session beans, executes some calls to
 * stress state replication, waits for passivation and exipration to kick
 * in, and then updates the sessions to produce the session removal
 * conflict seen in JBAS-1560 (Clustered stateful session bean removal of
 * expired passivated instances causes deadlock). This is sensative to
 * timing issues so a failure in activation can show up;
 * we catch any NoSuchObjectException to handle this.
 *
 * Migrated StatefulPassivationExpirationUnitTestCase from AS5 testsuite.
 *
 * @author Jan Martiska / jmartisk@redhat.com
 */
@RunWith(Arquillian.class)
public class StatefulPassivationExpirationTestCase {

    private static Logger log = Logger.getLogger(StatefulPassivationExpirationTestCase.class);
    private static final String ARCHIVE_NAME = "stateful-passivation-expiration";


    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;

    @Deployment()
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addClasses(StatefulBeanBase.class, StatefulBeanWithStatefulTimeout.class, StatefulRemote.class, StatefulRemoteHome.class);
        war.addClass(StatefulPassivationExpirationTestCase.class);
        log.info(war.toString(true));
        return war;
    }


    @Test
    public void testStatefulPassivateion() throws NamingException, CreateException, IOException {
        log.info("+++ testStatefulPassivationExpiration");


        InitialContext context = new InitialContext();

        int beanCount = 500;
        StatefulRemoteHome home = (StatefulRemoteHome) context.lookup("ejb:/" + ARCHIVE_NAME + "/" + StatefulBeanWithStatefulTimeout.class.getSimpleName() + "!"
                + StatefulRemoteHome.class.getName());

        long start = System.currentTimeMillis();
        log.info("Start bean creation");
        StatefulRemote[] beans = new StatefulRemote[beanCount];
        long[] accessStamp = new long[beanCount];
        for(int n = 0; n < beans.length; n ++)
        {
            beans[n] = home.create();
            accessStamp[n] = System.currentTimeMillis();
        }
        long end = System.currentTimeMillis();
        log.info("End bean creation, elapsed="+(end - start));

        int N = 5000;
        long min = 99999, max = 0, maxInactive = 0;
        for(int n = 0; n < N; n ++)
        {
            int id = n % beans.length;
            StatefulRemote bean = beans[id];
            if (bean == null)
                continue;  // bean timed out and removed
            long callStart = System.currentTimeMillis();
            long inactive = callStart - accessStamp[id];
            try
            {
                log.info("Setting number for id=" + id);
                bean.setNumber(id);
                log.info("Retrieving number for id=" + id);
                int number = bean.getNumber();

                long now = System.currentTimeMillis();
                long elapsed = now - callStart;
                accessStamp[id] = now;
                assertTrue("Id == "+id, number == id);
                min = Math.min(min, elapsed);
                max = Math.max(max, elapsed);
                maxInactive = Math.max(maxInactive, inactive);
                log.info(n+", elapsed="+elapsed+", inactive="+inactive);
            }
            catch (NoSuchObjectException nso)
            {
                log.info("Caught NoSuchObjectException on bean id=" + id + " -- inactive time = " + inactive);
                // Remove the bean as it will never succeed again
                beans[id] = home.create();            
            }
        }
        log.info(N+" calls complete, max="+max+", min="+min+", maxInactive="+maxInactive);

        log.info("WAITING now");
        try {
            Thread.sleep(15000);
        } catch (Exception e) {

        }

        for(int n = 0; n < beans.length; n ++)
        {
            try
            {
                if (beans[n] != null)
                    beans[n].remove();
            }
            catch (java.rmi.NoSuchObjectException nso)
            {
                log.info("Caught NoSuchObjectException removing bean " + n);
            } catch (RemoveException e) {
                log.info("Caught RemoveException removing bean " + n);
            }
        }

    }

}
