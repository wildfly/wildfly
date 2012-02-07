/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.single.jdbcstore;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Verify the integration between JdbcCacheStore and XADataSource. XADataSource's 
 * connection should be properly enlisted with the ongoing transaction when 
 * Infinispan internally calls its getConnection() method.
 * 
 * These tests should be failing until ISPN-604 is resolved.
 * 
 * @author Martin Gencur
 */
@RunWith(Arquillian.class)
public class TransactionalJdbcStoreTestCase {

    @Deployment
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        war.addClasses(TransactionalJdbcStoreTestCase.class, TransactionalInfinispanManagedBean.class);
        war.addAsManifestResource(new StringAsset("Manifest-Version: 1.0\nDependencies: org.infinispan export\n"),
                "MANIFEST.MF");
        return war;
    }

    @Ignore("ISPN-604")
    @Test
    public void testTxPutCommit() throws Exception {
        getIspnBeanFromJndi().testTxPutCommit();
    }

    @Ignore("ISPN-604")
    @Test
    public void testTxPutRollback() throws Exception {
        getIspnBeanFromJndi().testTxPutRollback();
    }

    @Ignore("ISPN-604")
    @Test
    public void testTxRemoveCommit() throws Exception {
        getIspnBeanFromJndi().testTxRemoveCommit();
    }

    @Ignore("ISPN-604")
    @Test
    public void testTxRemoveRollback() throws Exception {
        getIspnBeanFromJndi().testTxRemoveRollback();
    }

    @Ignore("ISPN-604")
    @Test
    public void testTxAlterCommit() throws Exception {
        getIspnBeanFromJndi().testTxAlterCommit();
    }

    @Ignore("ISPN-604")
    @Test
    public void testTxAlterRollback() throws Exception {
        getIspnBeanFromJndi().testTxAlterRollback();
    }

    private TransactionalInfinispanManagedBean getIspnBeanFromJndi() {
        InitialContext context;
        Object result;
        try {
            context = new InitialContext();
            result = context.lookup("java:module/infinispan");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        Assert.assertTrue(result instanceof TransactionalInfinispanManagedBean);
        TransactionalInfinispanManagedBean bean = (TransactionalInfinispanManagedBean) result;
        return bean;
    }
}
