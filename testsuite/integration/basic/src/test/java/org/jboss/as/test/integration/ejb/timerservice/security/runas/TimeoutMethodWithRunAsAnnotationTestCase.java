/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.security.runas;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.wildfly.security.auth.permission.ChangeRoleMapperPermission;
import org.wildfly.security.permission.ElytronPermission;

@ExtendWith(ArquillianExtension.class)
public class TimeoutMethodWithRunAsAnnotationTestCase {
    private static final Logger log = Logger.getLogger(TimeoutMethodWithRunAsAnnotationTestCase.class.getName());

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "myJar.jar");
        jar.addPackage(TimeoutMethodWithRunAsAnnotationTestCase.class.getPackage());
        jar.addAsManifestResource(createPermissionsXmlAsset(
                new ElytronPermission("setRunAsPrincipal"),
                new ChangeRoleMapperPermission("ejb")
                ), "permissions.xml");
        return jar;
    }

    @Test
    public void testTimeoutMethodWithRunAsAnnotation() throws Exception {
        final TimerBean timerBean = InitialContext.doLookup("java:module/TimerBean");
        timerBean.startTimer();
        Assertions.assertTrue(TimerBean.awaitTimerCall());
        Assertions.assertTrue(SecureBean.getSecureMethodRun());
    }

}