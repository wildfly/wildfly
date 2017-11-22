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
package org.jboss.as.test.integration.deployment.jcedeployment;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.net.URL;
import java.security.SecurityPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.deployment.jcedeployment.provider.DummyProvider;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This tests a JCE provider bundled in an EAR.
 * See AS7-6068 for more details.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JCETestCase {
    private static final Logger log = Logger.getLogger(JCETestCase.class);

    @Deployment
    public static Archive<?> deployment() throws Exception {
        final JavaArchive jce = ShrinkWrap.create(JavaArchive.class, "jcetest.jar")
                .addPackage(DummyProvider.class.getPackage());
        final File jceJar = new File("target/jcetest.jar");
        jce.as(ZipExporter.class).exportTo(jceJar, true);
        final File signedJceJar = new File("target/jcetestsigned.jar");
        JavaArchive signedJce;
        if (isJCETestable())  {
            // see genkey-jcetest-keystore in pom.xml for the keystore creation
            final JarSignerUtil signer = new JarSignerUtil(new File("target/jcetest.keystore"), "password", "password", /* alias */ "test");
            signer.sign(jceJar, signedJceJar);
            signer.verify(signedJceJar);
            signedJce = ShrinkWrap.create(ZipImporter.class, "jcetestsigned.jar")
                .importFrom(signedJceJar).as(JavaArchive.class);
        } else {
            log.trace("skipping the test since it can run on Oracle JDK only");
            signedJce = jce;
        }
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(ControllerServlet.class);

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "jcetest.ear")
                .addAsLibrary(signedJce)
                .addAsModule(war)
                .addAsManifestResource(JCETestCase.class.getPackage(), "jboss-deployment-structure.xml", "jboss-deployment-structure.xml");

        // ControllerServlet and DummyProvider need the following perms for their "dirty" game
        ear.addAsManifestResource(createPermissionsXmlAsset(
                new FilePermission("target/jcetest.keystore", "read"),
                new RuntimePermission("accessDeclaredMembers"),
                new ReflectPermission("suppressAccessChecks"),
                new RuntimePermission("accessClassInPackage.sun.security.validator"),
                new SecurityPermission("putProviderProperty.DP"),
                new SecurityPermission("insertProvider"),
                new RuntimePermission("getProtectionDomain")
        ), "permissions.xml");

        return ear;
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testJCE() throws Exception {
        Assume.assumeTrue(
                "Skipping test as 'javax.crypto.JarVerifier' does not contain a field called 'providerValidator'.",
                isJCETestable());

        String result = performCall(url, "controller");
        assertEquals("ok", result);
    }

    private String performCall(final URL url, final String urlPattern) throws Exception {
        return HttpRequest.get(url.toExternalForm() + urlPattern, 1000, SECONDS);
    }

    private static boolean isJCETestable() {
        try {
            final Class<?> cls = Class.forName("javax.crypto.JarVerifier");
            cls.getDeclaredField("providerValidator");
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
