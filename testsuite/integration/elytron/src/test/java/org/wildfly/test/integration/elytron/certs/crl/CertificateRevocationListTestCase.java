/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.certs.crl;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic simple integration tests to check CRL functionality.
 *
 * @author Jan Stourac <jstourac@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
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
    @Test
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
