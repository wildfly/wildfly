/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jca.security;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;

/**
 * Test for RA with security domain JBQA-5953
 *
 * @author <a href="mailto:vrastsel@redhat.com"> Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(RaWithSecurityDomainTestCase.RaWithSecurityDomainTestCaseSetup.class)
public class RaWithSecurityDomainTestCase {

    static class RaWithSecurityDomainTestCaseSetup extends AbstractLoginModuleSecurityDomainTestCaseSetup {

        @Override
        protected String getSecurityDomainName() {
            return "RaRealm";
        }

        @Override
        protected String getLoginModuleName() {
            return "ConfiguredIdentity";
        }

        @Override
        protected boolean isRequired() {
            return true;
        }

        @Override
        protected Map<String, String> getModuleOptions() {
            Map<String, String> moduleOptions = new HashMap<String, String>();
            moduleOptions.put("userName", "sa");
            moduleOptions.put("password", "sa");
            moduleOptions.put("principal", "sa");
            return moduleOptions;
        }
    }

    @Deployment
    public static Archive<?> deploymentSingleton() {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "single.jar").addClass(RaWithSecurityDomainTestCase.class)
                .addPackage(MultipleConnectionFactory1.class.getPackage());
        jar.addClasses(AbstractLoginModuleSecurityDomainTestCaseSetup.class, AbstractSecurityDomainSetup.class);
        final ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "test.rar").addAsLibrary(jar)
                .addAsResource(RaWithSecurityDomainTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(RaWithSecurityDomainTestCase.class.getPackage(), "roles.properties", "roles.properties")
                .addAsManifestResource("jca/security/ra/ra.xml", "ra.xml")
                .addAsManifestResource("jca/security/ra/ironjacamar.xml", "ironjacamar.xml");

        return rar;
    }

    @Resource(mappedName = "java:jboss/name1")
    private MultipleConnectionFactory1 connectionFactory1;

    @Test
    public void deploymentTest() throws Exception {
        assertNotNull("CF1 not found", connectionFactory1);
        assertNotNull("Cannot obtain connection", connectionFactory1.getConnection());
    }
}
