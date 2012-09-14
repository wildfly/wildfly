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
package org.jboss.as.test.integration.osgi.jta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.InputStream;

import javax.inject.Inject;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * An example of OSGi JTA.
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Oct-2009
 */
@RunWith(Arquillian.class)
public class TransactionTestCase {

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-jta");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(TransactionManager.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testUserTransaction() throws Exception {
        bundle.start();
        BundleContext context = bundle.getBundleContext();

        Transactional txObj = new Transactional();

        ServiceReference userTxRef = context.getServiceReference(UserTransaction.class.getName());
        assertNotNull("UserTransaction service not null", userTxRef);

        UserTransaction userTx = (UserTransaction) context.getService(userTxRef);
        assertNotNull("UserTransaction not null", userTx);

        userTx.begin();
        try {
            ServiceReference tmRef = context.getServiceReference(TransactionManager.class.getName());
            assertNotNull("TransactionManager service not null", tmRef);

            TransactionManager tm = (TransactionManager) context.getService(tmRef);
            assertNotNull("TransactionManager not null", tm);

            Transaction tx = tm.getTransaction();
            assertNotNull("Transaction not null", tx);

            tx.registerSynchronization(txObj);

            txObj.setMessage("Donate $1.000.000");
            assertNull("Uncommited message null", txObj.getMessage());

            userTx.commit();
        } catch (Exception ex) {
            userTx.setRollbackOnly();
            throw ex;
        }

        assertEquals("Donate $1.000.000", txObj.getMessage());
    }

    class Transactional implements Synchronization {
        private String volatileMessage;
        private String message;

        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            if (status == Status.STATUS_COMMITTED)
                message = volatileMessage;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.volatileMessage = message;
        }
    }
}