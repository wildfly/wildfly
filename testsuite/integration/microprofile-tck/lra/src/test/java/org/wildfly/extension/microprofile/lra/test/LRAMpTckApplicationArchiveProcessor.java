/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.lra.test;

import io.narayana.lra.LRAConstants;
import io.narayana.lra.arquillian.spi.NarayanaLRARecovery;
import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author Martin Stefanko
 */
public class LRAMpTckApplicationArchiveProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (applicationArchive instanceof WebArchive) {
            JavaArchive extensionsJar = ShrinkWrap.create(JavaArchive.class, "extension.jar");

            extensionsJar.addClasses(NarayanaLRARecovery.class, LRAConstants.class);
            extensionsJar.addAsServiceProvider(LRARecoveryService.class, NarayanaLRARecovery.class);

            extensionsJar.addAsManifestResource(new StringAsset("<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"\n" +
                "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "       xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd\"\n" +
                "       version=\"4.0\" bean-discovery-mode=\"all\">\n" +
                "</beans>"), "beans.xml");

            WebArchive war = (WebArchive) applicationArchive;
            war.addAsLibraries(extensionsJar);
        }
    }
}
