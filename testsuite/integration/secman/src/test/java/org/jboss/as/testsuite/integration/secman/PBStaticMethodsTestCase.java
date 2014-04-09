/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.testsuite.integration.secman;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.security.AccessControlException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.security.ExternalPasswordCache;
import org.jboss.security.Util;
import org.jboss.security.config.SecurityConfiguration;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case, which checks if static methods in PicketBox are protected by permission checks.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
public class PBStaticMethodsTestCase {

    /**
     * Creates test archive.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment()
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "pbsm.war");
    }

    // SecurityConfiguration ---------------------------------------------------

    /**
     * Test method for
     * {@link org.jboss.security.config.SecurityConfiguration#addApplicationPolicy(org.jboss.security.config.ApplicationPolicy)}
     * .
     */
    @Test
    public void testAddApplicationPolicy() {
        try {
            SecurityConfiguration.addApplicationPolicy(null);
            fail("Access should be denied");
        } catch (AccessControlException e) {
            RuntimePermission expectedPerm = new RuntimePermission(
                    "org.jboss.security.config.SecurityConfiguration.addApplicationPolicy");
            assertEquals("Permission type doesn't match", expectedPerm, e.getPermission());
        }
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#removeApplicationPolicy(java.lang.String)}.
     */
    @Test(expected = AccessControlException.class)
    public void testRemoveApplicationPolicy() {
        SecurityConfiguration.removeApplicationPolicy("test");
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#getApplicationPolicy(java.lang.String)}.
     */
    @Test(expected = AccessControlException.class)
    public void testGetApplicationPolicy() {
        SecurityConfiguration.getApplicationPolicy("test");
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#getCipherAlgorithm()}.
     */
    @Test(expected = AccessControlException.class)
    public void testGetCipherAlgorithm() {
        SecurityConfiguration.getCipherAlgorithm();
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#setCipherAlgorithm(java.lang.String)}.
     */
    @Test(expected = AccessControlException.class)
    public void testSetCipherAlgorithm() {
        SecurityConfiguration.setCipherAlgorithm(null);
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#getCipherKey()}.
     */
    @Test(expected = AccessControlException.class)
    public void testGetCipherKey() {
        SecurityConfiguration.getCipherKey();
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#setCipherKey(java.security.Key)}.
     */
    @Test(expected = AccessControlException.class)
    public void testSetCipherKey() {
        SecurityConfiguration.setCipherKey(null);
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#getCipherSpec()}.
     */
    @Test(expected = AccessControlException.class)
    public void testGetCipherSpec() {
        SecurityConfiguration.getCipherSpec();
    }

    /**
     * Test method for
     * {@link org.jboss.security.config.SecurityConfiguration#setCipherSpec(java.security.spec.AlgorithmParameterSpec)}.
     */
    @Test(expected = AccessControlException.class)
    public void testSetCipherSpec() {
        SecurityConfiguration.setCipherSpec(null);
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#getIterationCount()}.
     */
    @Test(expected = AccessControlException.class)
    public void testGetIterationCount() {
        SecurityConfiguration.getIterationCount();
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#setIterationCount(int)}.
     */
    @Test(expected = AccessControlException.class)
    public void testSetIterationCount() {
        SecurityConfiguration.setIterationCount(0);
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#getSalt()}.
     */
    @Test(expected = AccessControlException.class)
    public void testGetSalt() {
        SecurityConfiguration.getSalt();
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#setSalt(java.lang.String)}.
     */
    @Test(expected = AccessControlException.class)
    public void testSetSalt() {
        SecurityConfiguration.setSalt(null);
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#getKeyStoreType()}.
     */
    @Test(expected = AccessControlException.class)
    public void testGetKeyStoreType() {
        SecurityConfiguration.getKeyStoreType();
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#setKeyStoreType(java.lang.String)}.
     */
    @Test(expected = AccessControlException.class)
    public void testSetKeyStoreType() {
        SecurityConfiguration.setKeyStoreType(null);
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#getKeyStoreURL()}.
     */
    @Test(expected = AccessControlException.class)
    public void testGetKeyStoreURL() {
        SecurityConfiguration.getKeyStoreURL();
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#setKeyStoreURL(java.lang.String)}.
     */
    @Test(expected = AccessControlException.class)
    public void testSetKeyStoreURL() {
        SecurityConfiguration.setKeyStoreURL(null);
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#getKeyStorePass()}.
     */
    @Test(expected = AccessControlException.class)
    public void testGetKeyStorePass() {
        SecurityConfiguration.getKeyStorePass();
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#setKeyStorePass(java.lang.String)}.
     */
    @Test(expected = AccessControlException.class)
    public void testSetKeyStorePass() {
        SecurityConfiguration.setKeyStorePass(null);
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#getTrustStoreType()}.
     */
    @Test(expected = AccessControlException.class)
    public void testGetTrustStoreType() {
        SecurityConfiguration.getTrustStoreType();
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#setTrustStoreType(java.lang.String)}.
     */
    @Test(expected = AccessControlException.class)
    public void testSetTrustStoreType() {
        SecurityConfiguration.setTrustStoreType(null);
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#getTrustStorePass()}.
     */
    @Test(expected = AccessControlException.class)
    public void testGetTrustStorePass() {
        SecurityConfiguration.getTrustStorePass();
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#setTrustStorePass(java.lang.String)}.
     */
    @Test(expected = AccessControlException.class)
    public void testSetTrustStorePass() {
        SecurityConfiguration.setTrustStorePass(null);
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#getTrustStoreURL()}.
     */
    @Test(expected = AccessControlException.class)
    public void testGetTrustStoreURL() {
        SecurityConfiguration.getTrustStoreURL();
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#setTrustStoreURL(java.lang.String)}.
     */
    @Test(expected = AccessControlException.class)
    public void testSetTrustStoreURL() {
        SecurityConfiguration.setTrustStoreURL(null);
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#isDeepCopySubjectMode()}.
     */
    @Test(expected = AccessControlException.class)
    public void testIsDeepCopySubjectMode() {
        SecurityConfiguration.isDeepCopySubjectMode();
    }

    /**
     * Test method for {@link org.jboss.security.config.SecurityConfiguration#setDeepCopySubjectMode(boolean)}.
     */
    @Test(expected = AccessControlException.class)
    public void testSetDeepCopySubjectMode() {
        SecurityConfiguration.setDeepCopySubjectMode(false);
    }

    // ExternalPasswordCache ---------------------------------------------------

    /**
     * Test method for {@link org.jboss.security.ExternalPasswordCache#getExternalPasswordCacheInstance()}.
     */
    @Test
    public void testGetExternalPasswordCacheInstance() {
        try {
            ExternalPasswordCache.getExternalPasswordCacheInstance();
            fail("Access should be denied");
        } catch (AccessControlException e) {
            RuntimePermission expectedPerm = new RuntimePermission(
                    "org.jboss.security.ExternalPasswordCache.getExternalPasswordCacheInstance");
            assertEquals("Permission type doesn't match", expectedPerm, e.getPermission());
        }
    }

    // Util --------------------------------------------------------------------

    /**
     * Test method for {@link org.jboss.security.Util#loadPassword(String)}.
     *
     * @throws Exception
     */
    @Test
    public void testLoadPassword() throws Exception {
        try {
            Util.loadPassword("cat /etc/passwd");
            fail("Access should be denied");
        } catch (AccessControlException e) {
            RuntimePermission expectedPerm = new RuntimePermission("org.jboss.security.Util.loadPassword");
            assertEquals("Permission type doesn't match", expectedPerm, e.getPermission());
        }
    }
}
