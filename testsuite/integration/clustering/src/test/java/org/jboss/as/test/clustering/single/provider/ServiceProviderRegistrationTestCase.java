/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.provider;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.*;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.PropertyPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.provider.bean.ServiceProviderRetriever;
import org.jboss.as.test.clustering.cluster.provider.bean.ServiceProviderRetrieverBean;
import org.jboss.as.test.clustering.cluster.provider.bean.legacy.LegacyServiceProviderRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates that a service provider registration works in a non-clustered environment.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class ServiceProviderRegistrationTestCase {
    private static final String MODULE_NAME = ServiceProviderRegistrationTestCase.class.getSimpleName();

    @Deployment(testable = false)
    public static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(ServiceProviderRetrieverBean.class.getPackage());
        war.addPackage(LegacyServiceProviderRetrieverBean.class.getPackage());
        war.addAsManifestResource(createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read"), new RuntimePermission("getClassLoader")), "permissions.xml");
        war.setWebXML(org.jboss.as.test.clustering.cluster.provider.ServiceProviderRegistrationTestCase.class.getPackage(), "web.xml");
        return war;
    }

    @Test
    public void test() throws Exception {
        this.test(ServiceProviderRetrieverBean.class);
    }

    @Test
    public void legacy() throws Exception {
        this.test(LegacyServiceProviderRetrieverBean.class);
    }

    public void test(Class<? extends ServiceProviderRetriever> beanClass) throws Exception {
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            ServiceProviderRetriever bean = directory.lookupStateless(beanClass, ServiceProviderRetriever.class);
            Collection<String> names = bean.getProviders();
            assertEquals(1, names.size());
            assertTrue(names.toString(), names.contains(NODE_1));
        }
    }
}
