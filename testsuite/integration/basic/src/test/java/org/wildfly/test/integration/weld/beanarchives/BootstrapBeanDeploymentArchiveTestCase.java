/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.wildfly.test.integration.weld.beanarchives;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import javax.enterprise.inject.spi.Extension;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * See also <a href="https://issues.jboss.org/browse/WFLY-7025">WFLY-7025</a>.
 *
 * @author Martin Kouba
 */
@RunWith(Arquillian.class)
public class BootstrapBeanDeploymentArchiveTestCase {

    @Deployment
    public static Archive<?> createTestArchive() {
        return ShrinkWrap.create(WebArchive.class).addPackage(BootstrapBeanDeploymentArchiveTestCase.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsServiceProvider(Extension.class, TestExtension.class)
                .addAsManifestResource(createPermissionsXmlAsset(new RuntimePermission("accessDeclaredMembers")),"permissions.xml");
    }

    @Test
    public void testDeployment(@Alpha Map<String, String> alphaMap, @Bravo Map<String, String> bravoMap) throws NamingException {
        // Test that the deployment does not fail due to non-unique bean deployment identifiers and also the custom beans
        assertEquals(alphaMap.get("foo"), bravoMap.get("foo"));
    }

}
