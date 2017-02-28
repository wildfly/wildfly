/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.transaction.bmt.timeout;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.transactions.TransactionTestLookupUtil;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.PropertyPermission;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * BMT test where transaction timeout is involved.
 * The bean uses mock XAResources to simulate to get 2PC happen.
 */
@RunWith(Arquillian.class)
public class StatelessTimeoutTestCase {

    @ArquillianResource
    private InitialContext initCtx;

    @Inject
    private TransactionCheckerSingleton checker;

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "stateless-btm-txn-timeout.jar")
            .addPackage(StatelessTimeoutTestCase.class.getPackage())
            .addPackage(TxTestUtil.class.getPackage())
            .addClasses(TimeoutUtil.class)
            .addAsManifestResource(new StringAsset("<beans></beans>"),  "beans.xml")
            // grant necessary permissions for -Dsecurity.manager
            .addAsResource(createPermissionsXmlAsset(
                new PropertyPermission("ts.timeout.factor", "read")), "META-INF/jboss-permissions.xml");
        return jar;
    }

    @Before
    public void startUp() throws NamingException {
        checker.resetAll();
    }

    /**
     * Calling BTM method where transaction timeout is not modified.
     * Call is expected to be processed.
     */
    @Test
    public void noTimeout() throws Exception {
        StatelessBmtBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatelessBmtBean.class);
        bean.testTransaction(1, 0);
    }

    /**
     * Calling BTM method where 3 transactions are started and one of them timeouts.
     * Each transaction contains 2 XA Resources.
     * <br>
     * The assertion checks that 2 transactions were committed and one rolled-back.
     */
    @Test
    public void timeout() throws Exception {
        StatelessBmtBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatelessBmtBean.class);
        bean.testTransaction(3, 1);

        Assert.assertEquals("Two times two XA resources - for each commit happened", 4, checker.getCommitted());
        Assert.assertEquals("One time two XA resources - for each rollback happened", 2, checker.getRolledback());
    }

    /**
     * Calling BTM method where 4 transactions are started and 3 of them timeouts.
     * Each transaction contains 2 XA Resources.
     * <br>
     * The assertion checks that 1 transaction was committed and 3 rolled-back.
     */
    @Test
    public void timeoutMultiple() throws Exception {
        StatelessBmtBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatelessBmtBean.class);
        bean.testTransaction(4, 3);

        Assert.assertEquals("One time two XA resources - for each commit happened", 2, checker.getCommitted());
        Assert.assertEquals("Three times two XA resources - for each rollback happened", 6, checker.getRolledback());
    }

    /**
     * Calling BTM method where transaction timeout is defined small enough to get transaction timeout happens.
     * Such transaction is rolled-back.
     * Then transaction timeout is set to default value and other transaction is started which successfully commits.
     */
    @Test
    public void resetTimeoutToDefault() throws Exception {
        StatelessBmtRestartTimeoutBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatelessBmtRestartTimeoutBean.class);
        bean.test();
    }

}
