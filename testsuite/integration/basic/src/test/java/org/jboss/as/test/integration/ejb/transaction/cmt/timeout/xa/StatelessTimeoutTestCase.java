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

package org.jboss.as.test.integration.ejb.transaction.cmt.timeout.xa;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ejb.transaction.utils.SingletonChecker;
import org.jboss.as.test.integration.ejb.transaction.utils.TestLookupUtil;
import org.jboss.as.test.integration.ejb.transaction.utils.TxTestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests on transaction timeout behavior with Stateless beans.
 */
@RunWith(Arquillian.class)
public class StatelessTimeoutTestCase {

    @ArquillianResource
    private InitialContext initCtx;

    @Inject
    private SingletonChecker checker;
    

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "stateless-txn-timeout.jar")
            .addPackage(StatelessTimeoutTestCase.class.getPackage())
            .addPackage(TxTestUtil.class.getPackage())
            .addAsManifestResource(new StringAsset("<beans></beans>"),  "beans.xml");
        return jar;
    }

    @Before
    public void startUp() throws NamingException {
        checker.resetAll();
    }

    @Test
    public void noTimeout() throws Exception {
        
        StatelessWithAnnotationBean bean = TestLookupUtil.lookupModule(initCtx, StatelessWithAnnotationBean.class);
        bean.testTransaction();
        bean.testTransaction();

        Assert.assertFalse("Synchronization after begin should not be called", checker.isSynchronizedBegin());
        Assert.assertFalse("Synchronization before completion should not be called", checker.isSynchronizedBefore());
        Assert.assertFalse("Synchronization after completion should not be called", checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting two XA resources for each commit happened", 4, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }
}
