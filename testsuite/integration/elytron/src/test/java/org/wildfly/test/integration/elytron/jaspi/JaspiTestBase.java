/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.jaspi;

import java.net.URL;
import java.security.Permission;
import java.security.SecurityPermission;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.test.integration.elytron.ejb.base.WhoAmIBean;

abstract class JaspiTestBase {

    @ArquillianResource
    protected URL url;

    final boolean ejbSupported = !Boolean.getBoolean("ts.layers") && !Boolean.getBoolean("ts.bootable");

    protected static WebArchive createDeployment(final String name) {
        final Package testPackage = ConfiguredJaspiTestCase.class.getPackage();
        final Permission[] permissions = new Permission[] {
                    new SecurityPermission("getProperty.authconfigprovider.factory"),
                    new SecurityPermission("setProperty.authconfigfactory.provider")
                };
        return ShrinkWrap.create(WebArchive.class, name + ".war")
                .addClasses(JaspiTestServlet.class, SimpleServerAuthModule.class, WhoAmI.class)
                .addClasses(WhoAmI.class, WhoAmIBean.class, WhoAmIBeanImpl.class)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset("JaspiDomain"), "jboss-web.xml")
                .addAsWebInfResource(testPackage, "web.xml", "web.xml")
                .addAsWebInfResource(testPackage, "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(permissions), "permissions.xml");
    }
}
