/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.naming.dns;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;

import java.net.SocketPermission;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

/**
 * Test which ensures DNS Context is available in JNDI.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@RunWith(Arquillian.class)
public class DnsContextLookupTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive getDeployment() {
        return ShrinkWrap.create(WebArchive.class, DnsContextLookupTestCase.class.getSimpleName() + ".war")
            .addClasses(DnsContextLookupTestCase.class, DnsContextLookupBean.class)
            .addAsManifestResource(createPermissionsXmlAsset(
                    new SocketPermission("*:10389", "connect,resolve"),
                    new RuntimePermission("accessClassInPackage.com.sun.jndi.dns"),
                    new RuntimePermission("accessClassInPackage.com.sun.jndi.url.dns")
            ), "permissions.xml");
    }

    @Test
    public void testTaskSubmit() throws Exception {
        final DnsContextLookupBean bean =  InitialContext.doLookup("java:module/" + DnsContextLookupBean.class.getSimpleName());
        bean.testDnsContextLookup("8.8.8.8");
    }

}
