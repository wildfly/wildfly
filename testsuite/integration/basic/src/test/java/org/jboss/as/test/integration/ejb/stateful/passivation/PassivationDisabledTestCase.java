/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.stateful.passivation;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.util.PropertyPermission;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests to validate behavior of EJB 3.2 passivationCapable flag.
 * @author Paul Ferraro
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(PassivationTestCaseSetup.class)
public class PassivationDisabledTestCase {

    @ArquillianResource
    private InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, PassivationDisabledTestCase.class.getSimpleName() + ".jar");
        jar.addPackage(PassivationTestCase.class.getPackage());
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        jar.addAsManifestResource(PassivationTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addAsManifestResource(PassivationTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "jboss-permissions.xml");
        return jar;
    }

    /**
     * Tests that an EJB 3.2 stateful bean which is marked as <code>passivationCapable=false</code> isn't passivated or activated
     *
     * @throws Exception
     */
    @Test
    public void testPassivationDisabledBean() throws Exception {
        try (Bean bean = (Bean) ctx.lookup("java:module/" + PassivationDisabledBean.class.getSimpleName() + "!" + Bean.class.getName())) {
            bean.doNothing();
            // now do the same with a deployment descriptor configured stateful bean
            try (Bean ddBean = (Bean) ctx.lookup("java:module/passivation-disabled-bean" + "!" + Bean.class.getName())) {
                ddBean.doNothing();

                try (Bean bean2 = (Bean) ctx.lookup("java:module/" + PassivationDisabledBean.class.getSimpleName() + "!" + Bean.class.getName())) {
                    bean2.doNothing();
                    // now do the same with a deployment descriptor configured stateful bean
                    try (Bean ddBean2 = (Bean) ctx.lookup("java:module/passivation-disabled-bean" + "!" + Bean.class.getName())) {
                        ddBean2.doNothing();

                        // make sure bean's passivation and activation callbacks weren't invoked
                        Assertions.assertFalse(bean.wasPassivated(), "(Annotation based) Stateful bean marked as passivation disabled was incorrectly passivated");
                        Assertions.assertFalse(bean.wasActivated(), "(Annotation based) Stateful bean marked as passivation disabled was incorrectly activated");

                        Assertions.assertFalse(ddBean.wasPassivated(), "(Deployment descriptor based) Stateful bean marked as passivation disabled was incorrectly passivated");
                        Assertions.assertFalse(ddBean.wasActivated(), "(Deployment descriptor based) Stateful bean marked as passivation disabled was incorrectly activated");
                    }
                }
            }
        }
    }

    /**
     * Tests that an EJB 3.2 stateful bean which is marked as <code>passivationCapable=true</code> is passivated or activated
     *
     * @throws Exception
     */
    @Test
    public void testPassivationEnabledBean() throws Exception {
        try (Bean bean = (Bean) ctx.lookup("java:module/" + PassivationEnabledBean.class.getSimpleName() + "!" + Bean.class.getName())) {
            // make an invocation
            bean.doNothing();

            // now do the same with a deployment descriptor configured stateful bean
            try (Bean ddBean = (Bean) ctx.lookup("java:module/passivation-enabled-bean" + "!" + Bean.class.getName())) {
                ddBean.doNothing();

                // Create a 2nd set of beans, forcing the first set to passivate
                try (Bean bean2 = (Bean) ctx.lookup("java:module/" + PassivationEnabledBean.class.getSimpleName() + "!" + Bean.class.getName())) {
                    bean2.doNothing();

                    try (Bean ddBean2 = (Bean) ctx.lookup("java:module/passivation-enabled-bean" + "!" + Bean.class.getName())) {
                        ddBean2.doNothing();

                        Assertions.assertTrue(bean.wasPassivated(), "(Annotation based) Stateful bean marked as passivation enabled was not passivated");
                        Assertions.assertTrue(bean.wasActivated(), "(Annotation based) Stateful bean marked as passivation enabled was not activated");

                        Assertions.assertTrue(ddBean.wasPassivated(), "(Deployment descriptor based) Stateful bean marked as passivation enabled was not passivated");
                        Assertions.assertTrue(ddBean.wasActivated(), "(Deployment descriptor based) Stateful bean marked as passivation enabled was not activated");
                    }
                }
            }
        }
    }

    /**
     * Tests that an EJB 3.2 stateful bean which is marked as <code>passivationCapable=true</code> via annotation but overridden
     * as passivation disabled via deployment descriptor, isn't passivated or activated
     *
     * @throws Exception
     */
    @Test
    public void testPassivationDDOverrideBean() throws Exception {
        try (Bean passivationOverrideBean = (Bean) ctx.lookup("java:module/passivation-override-bean" + "!" + Bean.class.getName())) {
            // make an invocation
            passivationOverrideBean.doNothing();

            // Create a 2nd set of beans, that would normally force the first set to passivate
            try (Bean passivationOverrideBean2 = (Bean) ctx.lookup("java:module/passivation-override-bean" + "!" + Bean.class.getName())) {
                passivationOverrideBean2.doNothing();

                // make sure bean's passivation and activation callbacks weren't invoked
                Assertions.assertFalse(passivationOverrideBean.wasPassivated(), "(Annotation based) Stateful bean marked as passivation disabled was incorrectly passivated");
                Assertions.assertFalse(passivationOverrideBean.wasActivated(), "(Annotation based) Stateful bean marked as passivation disabled was incorrectly activated");
            }
        }
    }
}
