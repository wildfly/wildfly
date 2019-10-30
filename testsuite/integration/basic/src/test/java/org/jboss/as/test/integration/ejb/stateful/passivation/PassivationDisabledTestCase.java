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
package org.jboss.as.test.integration.ejb.stateful.passivation;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.PropertyPermission;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests to validate behavior of EJB 3.2 passivationCapable flag.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
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
                        Assert.assertFalse("(Annotation based) Stateful bean marked as passivation disabled was incorrectly passivated", bean.wasPassivated());
                        Assert.assertFalse("(Annotation based) Stateful bean marked as passivation disabled was incorrectly activated", bean.wasActivated());

                        Assert.assertFalse("(Deployment descriptor based) Stateful bean marked as passivation disabled was incorrectly passivated", ddBean.wasPassivated());
                        Assert.assertFalse("(Deployment descriptor based) Stateful bean marked as passivation disabled was incorrectly activated", ddBean.wasActivated());
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

                        Assert.assertTrue("(Annotation based) Stateful bean marked as passivation enabled was not passivated", bean.wasPassivated());
                        Assert.assertTrue("(Annotation based) Stateful bean marked as passivation enabled was not activated", bean.wasActivated());

                        Assert.assertTrue("(Deployment descriptor based) Stateful bean marked as passivation enabled was not passivated", ddBean.wasPassivated());
                        Assert.assertTrue("(Deployment descriptor based) Stateful bean marked as passivation enabled was not activated", ddBean.wasActivated());
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
                Assert.assertFalse("(Annotation based) Stateful bean marked as passivation disabled was incorrectly passivated", passivationOverrideBean.wasPassivated());
                Assert.assertFalse("(Annotation based) Stateful bean marked as passivation disabled was incorrectly activated", passivationOverrideBean.wasActivated());
            }
        }
    }
}
