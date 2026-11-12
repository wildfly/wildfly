/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.naming.rmi;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.naming.InitialContext;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

/**
 * Test which ensures RMI Context is available in JNDI.
 * @author Eduardo Martins
 */
@ExtendWith(ArquillianExtension.class)
public class RmiContextLookupTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive getDeployment() {
        return ShrinkWrap.create(WebArchive.class, RmiContextLookupTestCase.class.getSimpleName() + ".war")
            .addClasses(RmiContextLookupTestCase.class, RmiContextLookupBean.class)
            .addAsManifestResource(createPermissionsXmlAsset(new RuntimePermission("accessClassInPackage.com.sun.jndi.url.rmi")), "permissions.xml");
    }

    @Test
    public void testTaskSubmit() throws Exception {
        final RmiContextLookupBean bean =  InitialContext.doLookup("java:module/" + RmiContextLookupBean.class.getSimpleName());
        bean.testRmiContextLookup(managementClient.getMgmtAddress(), 11090);
    }

}
