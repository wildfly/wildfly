/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.single.registry;

import static org.jboss.as.test.clustering.ClusteringTestConstants.*;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.PropertyPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.cluster.registry.bean.RegistryRetriever;
import org.jboss.as.test.clustering.cluster.registry.bean.RegistryRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.util.DisableInvocationTestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates that a registry works in a non-clustered environment.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RegistryTestCase {
    private static final String MODULE_NAME = "registry";
    private static final String CLIENT_PROPERTIES = "cluster/ejb3/stateless/jboss-ejb-client.properties";

    @BeforeClass
    public static void beforeClass() {
        DisableInvocationTestUtil.disable();
    }

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(RegistryRetriever.class.getPackage());
        jar.addAsManifestResource(createPermissionsXmlAsset(
                new PropertyPermission(NODE_NAME_PROPERTY, "read")
        ), "permissions.xml");
        return jar;
    }

    @Test
    public void test() throws Exception {

        // TODO Elytron: Once support for legacy EJB properties has been added back, actually set the EJB properties
        // that should be used for this test using CLIENT_PROPERTIES and ensure the EJB client context is reset
        // to its original state at the end of the test
        EJBClientContextSelector.setup(CLIENT_PROPERTIES);

        try (EJBDirectory context = new RemoteEJBDirectory(MODULE_NAME)) {
            RegistryRetriever bean = context.lookupStateless(RegistryRetrieverBean.class, RegistryRetriever.class);
            Collection<String> names = bean.getNodes();
            assertEquals(1, names.size());
            assertTrue(names.toString(), names.contains(NODE_1));
        }
    }
}
