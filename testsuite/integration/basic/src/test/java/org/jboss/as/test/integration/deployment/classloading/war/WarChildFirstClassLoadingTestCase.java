/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.classloading.war;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import jakarta.ejb.Stateless;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
public class WarChildFirstClassLoadingTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addClasses(WarChildFirstClassLoadingTestCase.class, Stateless.class);
        war.addAsManifestResource(createPermissionsXmlAsset(new RuntimePermission("getClassLoader")), "permissions.xml");
        return war;
    }

    @Test
    public void testChildFirst() throws ClassNotFoundException {
        Assertions.assertNotSame(Stateless.class.getClassLoader(), getClass().getClassLoader());
    }

}
