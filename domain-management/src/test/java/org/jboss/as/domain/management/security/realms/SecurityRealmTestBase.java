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

package org.jboss.as.domain.management.security.realms;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.security.operations.SecurityRealmAddBuilder;
import org.jboss.as.domain.management.security.util.ManagementControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * An extension to {@link AbstractControllerTestBase} to add additional initialisation required for testing security realms.
 *
 * This class should take the controller to the point that security realms can be defined, sub classes / test cases will be
 * responsible for creating the actual realm definitions.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class SecurityRealmTestBase extends ManagementControllerTestBase {

    protected static final String TEST_REALM = "TestRealm";
    protected SecurityRealm securityRealm;

    @Test
    public void testRealmReady() {
        assertEquals("Security realm name", TEST_REALM, securityRealm.getName());
        assertTrue("Realm is ready", securityRealm.isReady());
    }

    @Before
    public void lookupSecurityRealm() {
        ServiceContainer container = getContainer();
        ServiceController<?> service = container.getRequiredService(SecurityRealm.ServiceUtil.createServiceName(TEST_REALM));

        securityRealm = (SecurityRealm) service.getValue();
    }

    @After
    public void clearSecurityRealm() {
        securityRealm = null;
    }

    @Override
    protected void addBootOperations(List<ModelNode> bootOperations) throws Exception {
        super.addBootOperations(bootOperations);

        SecurityRealmAddBuilder builder = SecurityRealmAddBuilder.builder(TEST_REALM);
        initialiseRealm(builder);
        bootOperations.add(builder.build());
    }

    protected abstract void initialiseRealm(SecurityRealmAddBuilder builder) throws Exception;

}
