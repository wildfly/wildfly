/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote;

import static org.junit.Assert.*;

import java.util.PropertyPermission;

import jakarta.ejb.EJBException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.IncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.InfinispanExceptionThrowingIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for WFLY-5788 and WFLY-9951.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
public class ClientExceptionRemoteEJBTestCase extends AbstractClusteringTestCase {
    private static final String MODULE_NAME = ClientExceptionRemoteEJBTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar")
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class, InfinispanExceptionThrowingIncrementorBean.class)
                .setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.infinispan.commons\n"))
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    @Test
    public void test() throws Exception {
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            Incrementor bean = directory.lookupStateful(InfinispanExceptionThrowingIncrementorBean.class, Incrementor.class);

            bean.increment();

            fail("Expected EJBException but didn't catch it");
        } catch (EJBException e) {
            assertNull("Cause of EJBException has not been removed", e.getCause());
        }
    }
}
