/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.certs.crl;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic simple integration tests to check CRL functionality.
 *
 * @author Jan Stourac <jstourac@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@Ignore("WFLY-14880 / WFCORE-5461 temporary disable for CI of WFCORE")
public class CertificateRevocationListTestCase extends CertificateRevocationListTestBase {

    /**
     * This test verifies a certificate *not* present in the CRL configured using
     * the certificate-revocation-list attribute gets accepted.
     */
    @Test
    public void testTwoWayCertificateNotInServerCrl() throws Exception {
        configureSSLContext(TWO_WAY_SSL_CONTEXT_NAME);

        setSoftFail(false);
        testCommon(goodCertKeyStore, trustStore, PASSWORD, true);

        setSoftFail(true);
        testCommon(goodCertKeyStore, trustStore, PASSWORD, true);

        restoreConfiguration();
    }

    /**
     * This test verifies certificate present in the CRL configured in the server
     * using the certificate-revocation-list attribute gets rejected.
     */
    @Test
    public void testTwoWayCertificateInServerCrl() throws Exception {
        configureSSLContext(TWO_WAY_SSL_CONTEXT_NAME);

        setSoftFail(false);
        testCommon(revokedCertKeyStore, trustStore, PASSWORD, false);

        setSoftFail(true);
        testCommon(revokedCertKeyStore, trustStore, PASSWORD, false);

        restoreConfiguration();
    }

    /**
     * This test verifies a certificate present in the single CRL configured using the certificate-revocation-lists
     * attribute gets rejected.
     */
    public void testTwoWayCertificateInServerCrls() throws Exception {
        configureSSLContext(TWO_WAY_SINGLE_CRL_SSL_CONTEXT);

        setSoftFail(false);
        testCommon(revokedCertKeyStore, trustStore, PASSWORD, false);

        setSoftFail(true);
        testCommon(revokedCertKeyStore, trustStore, PASSWORD, false);

        restoreConfiguration();
    }

    /**
     * This test verifies certificates present in any CRL configured with the
     * certificate-revocation-lists attribute gets rejected.
     */
    @Test
    public void testTwoWayCertificateInMultipleServerCrls() throws Exception {
        configureSSLContext(TWO_WAY_MULTIPLE_CRL_SSL_CONTEXT);

        setSoftFail(false);
        testCommon(revokedCertKeyStore, trustStore, PASSWORD, false);

        setSoftFail(true);
        testCommon(revokedCertKeyStore, trustStore, PASSWORD, false);

        restoreConfiguration();
    }

    /**
     * This test verifies certificates *not* present in any of the CRLs configured with
     * the certificate-revocation-lists attribute gets accepted.
     */
    @Test
    public void testTwoWayCertificateNotInMultipleServerCrls() throws Exception {
        configureSSLContext(TWO_WAY_MULTIPLE_CRL_SSL_CONTEXT);

        setSoftFail(false);
        testCommon(goodCertKeyStore, trustStore, PASSWORD, true);

        setSoftFail(true);
        testCommon(goodCertKeyStore, trustStore, PASSWORD, true);

        restoreConfiguration();
    }
}
