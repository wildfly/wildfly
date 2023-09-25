/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.jwt.test;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
//import org.jboss.shrinkwrap.api.ShrinkWrap;
//import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
//import org.testng.Assert;

/**
 * Class to handle additional processing needed for deployments.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DeploymentProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (archive instanceof WebArchive) {
            //JavaArchive extensionsJar = ShrinkWrap.create(JavaArchive.class, "extension.jar");

            //extensionsJar.addPackage(Assert.class.getPackage());

            WebArchive war = WebArchive.class.cast(archive);
            if (war.contains("META-INF/microprofile-config.properties") == false) {
                war.addAsManifestResource("META-INF/microprofile-config-local.properties", "microprofile-config.properties");
            }
            war.addAsWebInfResource("WEB-INF/web.xml", "web.xml");
            war.addAsWebInfResource("WEB-INF/jboss-web.xml", "jboss-web.xml");
            //war.addAsLibraries(extensionsJar);
        }

        System.out.printf("Final Srchive: %s\n", archive.toString(true));
    }
}