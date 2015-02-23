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
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.URL;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

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
        if (isOracleJVM())  {
            // see genkey-jcetest-keystore in pom.xml for the keystore creation
            final JarSignerUtil signer = new JarSignerUtil(new File("../jcetest.keystore"), "password", "password", /* alias */ "test");
            signer.sign(jceJar, signedJceJar);
            signer.verify(signedJceJar);
            signedJce = ShrinkWrap.create(ZipImporter.class, "jcetestsigned.jar")
                .importFrom(signedJceJar).as(JavaArchive.class);
        } else {
            log.info("skipping the test since it can run on Oracle JDK only");
            signedJce = jce;
        }
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(ControllerServlet.class);

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "jcetest.ear")
                .addAsLibrary(signedJce)
                .addAsModule(war)
                .addAsManifestResource(JCETestCase.class.getPackage(), "jboss-deployment-structure.xml", "jboss-deployment-structure.xml");
        return ear;
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testJCE() throws Exception {
        if(isOracleJVM()) {
            String result = performCall(url, "controller");
            assertEquals("ok", result);
        } else {
            log.info("skipping the test since it can run on Oracle JDK only");
        }
    }

    private String performCall(final URL url, final String urlPattern) throws Exception {
        return HttpRequest.get(url.toExternalForm() + urlPattern, 1000, SECONDS);
    }
    
    private static boolean isOracleJVM() {
        return "Oracle Corporation".equalsIgnoreCase(System.getProperty("java.vm.vendor"));
    }
}
