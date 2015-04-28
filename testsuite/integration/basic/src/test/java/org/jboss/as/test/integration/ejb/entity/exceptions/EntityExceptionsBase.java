/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.entity.exceptions;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

public abstract class EntityExceptionsBase {
    private static final String ARCHIVE_NAME = "ExceptionsBeanTest.war";

    static abstract class EntityPoolSetup implements ServerSetupTask {

        private final String poolName;
        private final boolean optimisticLock;

        public EntityPoolSetup(String poolName, boolean optimisticLock) {
            this.poolName = poolName;
            this.optimisticLock = optimisticLock;
        }

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            EJBManagementUtil.createStrictMaxPool(managementClient.getControllerClient(), poolName, 1, 1, TimeUnit.MILLISECONDS);
            EJBManagementUtil.editEntityBeanInstancePool(managementClient.getControllerClient(), poolName, optimisticLock);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            EJBManagementUtil.undefineEntityBeanInstancePool(managementClient.getControllerClient());
            EJBManagementUtil.removeStrictMaxPool(managementClient.getControllerClient(), poolName);
        }
    }

    static class OptimisticEntityPoolSetup extends EntityPoolSetup {
        public OptimisticEntityPoolSetup() {
            super("TestEntityOptimisticPool", true);
        }
    }

    static class PessimisticEntityPoolSetup extends EntityPoolSetup {
        public PessimisticEntityPoolSetup() {
            super("TestEntityPessimisticPool", false);
        }
    }

    @Deployment
    public static Archive deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME);
        war.addPackage(EntityOptimisticLockingExceptionsTestCase.class.getPackage());
        war.addAsWebInfResource(EntityOptimisticLockingExceptionsTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return war;
    }

    @ArquillianResource
    private InitialContext ctx;

    @SuppressWarnings("unchecked")
    protected  <T> T lookup(String name) throws NamingException {
        return (T) ctx.lookup(name);
    }

    protected ExceptionsLocalHome getLocalHome() throws NamingException {
        return lookup("java:module/ExceptionsBean!"+ExceptionsLocalHome.class.getName());
    }

    protected ExceptionsRemoteHome getRemoteHome() throws NamingException {
        return lookup("java:module/ExceptionsBean!"+ExceptionsRemoteHome.class.getName());
    }

    protected ExceptionsLocalInterface localFind(String key) throws Exception {
        return getLocalHome().findByPrimaryKey(key);
    }

    protected ExceptionsLocalInterface localFind() throws Exception {
        return localFind("find-test-local");
    }

    protected ExceptionsRemoteInterface remoteFind() throws Exception {
        return getRemoteHome().findByPrimaryKey("find-test-remote");
    }

    protected UserTransaction getUserTransaction() throws NamingException {
        return lookup("java:jboss/UserTransaction");
    }

    /**
     * Sanity check - verify actual pool limitations
     */
    @Test
    public void shouldNotReturnInstanceToPoolUntilTransactionEnd() throws Exception {
        UserTransaction ut = getUserTransaction();
        ut.begin();
        try {
            localFind("key1").test();
            try {
                localFind("key2").test();
                fail("Only one instance of bean should be allowed by pool");
            } catch (Exception e) {
                // Expected exception
            }
        } finally {
            ut.rollback();
        }
    }

    @Test
    public void shouldReleaseInstanceToPoolOnLocalCreateException() throws Exception {
        try {
            getLocalHome().create("create-test");
            fail("Create should throw exception");
        } catch (CreateException e) {
            // Expected exception
        } catch (EJBException e) {
            // Expected exception
        }

        // There should be no exception here
        localFind();
    }

    @Test
    public void shouldReleaseInstanceToPoolOnRemoteCreateException() throws Exception {
        try {
            getRemoteHome().create("create-test");
            fail("Create should throw exception");
        } catch (CreateException e) {
            // Expected exception
        } catch (RemoteException e) {
            // Expected exception
        }

        // There should be no exception here
        remoteFind();
    }

    @Test
    public void shouldReleaseInstanceToPoolOnLocalRuntimeException() throws Exception {
        try {
            localFind().throwRuntimeException();
            fail("business method should throw exception");
        } catch (EJBException e) {
            // Expected exception
        }

        // There should be no exception here
        localFind();
    }

    @Test
    public void shouldReleaseInstanceToPoolOnRemoteRuntimeException() throws Exception {
        try {
            remoteFind().throwRuntimeException();
            fail("business method should throw exception");
        } catch (RemoteException e) {
            // Expected exception
        }

        // There should be no exception here
        remoteFind();
    }
}
