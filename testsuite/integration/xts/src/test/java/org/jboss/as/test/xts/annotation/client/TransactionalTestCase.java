/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.xts.annotation.client;

import com.arjuna.mw.wst11.UserTransaction;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.File;
import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.util.PropertyPermission;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.xts.annotation.service.TransactionalService;
import org.jboss.as.test.xts.annotation.service.TransactionalServiceImpl;
import org.jboss.as.test.xts.util.DeploymentHelper;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(Arquillian.class)
@Ignore("[WFLY-15275] Fix XTS test cases without legacy security.")
public class TransactionalTestCase {

    private static final String DEPLOYMENT_NAME = "transactional-test";

    private static final String SERVER_HOST_PORT = TestSuiteEnvironment.getServerAddress() + ":"
            + TestSuiteEnvironment.getHttpPort();

    private static final String DEPLOYMENT_URL = "http://" + SERVER_HOST_PORT + "/" + DEPLOYMENT_NAME;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = DeploymentHelper.getInstance().getWebArchiveWithPermissions(DEPLOYMENT_NAME)
                .addClass(TransactionalClient.class)
                .addClass(TransactionalService.class)
                .addClass(TransactionalServiceImpl.class)
                .addClass(TestSuiteEnvironment.class)
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.xts,org.jboss.jts\n"), "MANIFEST.MF");
        if (SystemUtils.JAVA_VENDOR.startsWith("IBM")) {
                webArchive.addAsManifestResource(
                        createPermissionsXmlAsset(
                                //This is not catastrophic if absent
                                ///.../testsuite/integration/xts/xcatalog
                                //$JAVA_HOME/jre/conf/jaxm.properties
                                //$JAVA_HOME/jre/lib/jaxws.properties
                                //$JAVA_HOME/jre/conf/jaxws.properties
                                new FilePermission(System.getProperties().getProperty("jbossas.ts.integ.dir") + File.separator + "xts" + File.separator
                                        + "xcatalog", "read"),
                                new FilePermission(System.getenv().get("JAVA_HOME") + File.separator + "jre" + File.separator
                                        + "conf" + File.separator + "jaxm.properties", "read"),
                                new FilePermission(System.getenv().get("JAVA_HOME") + File.separator + "jre" + File.separator
                                        + "conf" + File.separator + "jaxws.properties", "read"),
                                new FilePermission(System.getenv().get("JAVA_HOME") + File.separator + "jre" + File.separator
                                        + "lib" + File.separator + "jaxws.properties", "read"),
                                new ReflectPermission("suppressAccessChecks"),
                                new RuntimePermission("accessDeclaredMembers"),
                                new RuntimePermission("accessClassInPackage.com.sun.org.apache.xerces.internal.jaxp"),
                                new PropertyPermission("arquillian.debug", "read"),
                                new PropertyPermission("node0", "read"),
                                new PropertyPermission("jboss.http.port", "read"),
                                new PropertyPermission("management.address", "read")),
                        "permissions.xml");
        }
        return webArchive;
    }

    @Test
    public void testNoTransaction() throws Exception {
        final String deploymentUrl = DEPLOYMENT_URL;
        final TransactionalService transactionalService = TransactionalClient.newInstance(deploymentUrl);

        final boolean isTransactionActive = transactionalService.isTransactionActive();

        Assert.assertEquals(false, isTransactionActive);
    }

    @Test
    public void testActiveTransaction() throws Exception {
        final String deploymentUrl = DEPLOYMENT_URL;
        final TransactionalService transactionalService = TransactionalClient.newInstance(deploymentUrl);
        final UserTransaction userTransaction = UserTransaction.getUserTransaction();

        userTransaction.begin();
        final boolean isTransactionActive = transactionalService.isTransactionActive();
        userTransaction.commit();

        Assert.assertEquals(true, isTransactionActive);
    }

}
