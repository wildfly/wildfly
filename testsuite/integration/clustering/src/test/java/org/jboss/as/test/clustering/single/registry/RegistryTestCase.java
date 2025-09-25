/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.registry;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.*;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.PropertyPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.registry.bean.RegistryRetriever;
import org.jboss.as.test.clustering.cluster.registry.bean.RegistryRetrieverBean;
import org.jboss.as.test.clustering.cluster.registry.bean.legacy.LegacyRegistryRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates that a registry works in a non-clustered environment.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class RegistryTestCase {
    private static final String MODULE_NAME = RegistryTestCase.class.getSimpleName();

    @Deployment(testable = false)
    public static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(RegistryRetrieverBean.class.getPackage());
        war.addPackage(LegacyRegistryRetrieverBean.class.getPackage());
        war.addAsManifestResource(createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read"), new RuntimePermission("getClassLoader")), "permissions.xml");
        war.setWebXML(RegistryTestCase.class.getPackage(), "web.xml");
        return war;
    }

    @Test
    public void test() throws Exception {
        this.test(RegistryRetrieverBean.class);
    }

    @Test
    public void legacy() throws Exception {
        this.test(LegacyRegistryRetrieverBean.class);
    }

    @Test
    public void test(Class<? extends RegistryRetriever> beanClass) throws Exception {
        try (EJBDirectory context = new RemoteEJBDirectory(MODULE_NAME)) {
            RegistryRetriever bean = context.lookupStateless(LegacyRegistryRetrieverBean.class, RegistryRetriever.class);
            Collection<String> names = bean.getNodes();
            assertEquals(1, names.size());
            assertTrue(names.toString(), names.contains(NODE_1));
        }
    }
}
