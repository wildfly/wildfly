/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
