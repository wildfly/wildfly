/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.certs.ocsp;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CliUtils;
import org.jboss.as.version.Stability;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.elytron.util.WelcomeContent;

/**
 * Tests to check OCSP feature when no OCSP responder is available.
 *
 * @author Jan Stourac <jstourac@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({OcspTestBase.OcspResponderDownServerSetup.class, WelcomeContent.SetupTask.class })
public class OcspResponderDownTestCase extends OcspTestBase {

    public OcspResponderDownTestCase() {
        super(Stability.DEFAULT);
    }
    /**
     * Let's check connection to server using a valid certificate. OCSP responder is disabled; CRL not configured.
     * Expected behavior: server rejects connection for soft-fail=false variant but accepts connection for
     * soft-fail=true variant.
     *
     * @throws Exception
     */
    @Test
    public void testOcspGoodResponderDown() throws Exception {
        setCrl(null);
        setSoftFail(false);
        testCommon(ocspCheckedGoodKeyStore, trustStore, PASSWORD, false);

        setSoftFail(true);
        testCommon(ocspCheckedGoodKeyStore, trustStore, PASSWORD, true);
    }

    /**
     * Let's check connection to server using a valid certificate. OCSP responder is disabled; CRL configured.
     * Expected behavior: server rejects connection for soft-fail=false variant but accepts connection for
     * soft-fail=true variant.
     *
     * @throws Exception
     */
    @Test
    public void testOcspGoodResponderDownCrlActive() throws Exception {
        setCrl(CliUtils.asAbsolutePath(CA_BLANK_PEM_CRL));

        setSoftFail(false);
        testCommon(ocspCheckedGoodKeyStore, trustStore, PASSWORD, true);

        setSoftFail(true);
        testCommon(ocspCheckedGoodKeyStore, trustStore, PASSWORD, true);
    }

    /**
     * Let's check connection to server using a revoked certificate. OCSP responder is disabled; CRL not configured.
     * Expected behavior: server rejects connection for both soft-fail values.
     *
     * @throws Exception
     */
    @Test
    public void testOcspRevokedResponderDown() throws Exception {
        setCrl(null);

        setSoftFail(false);
        testCommon(ocspCheckedRevokedKeyStore, trustStore, PASSWORD, false);

        setSoftFail(true);
        testCommon(ocspCheckedRevokedKeyStore, trustStore, PASSWORD, true);
    }

    /**
     * Let's check connection to server using a revoked certificate. OCSP responder is disabled; CRL configured.
     * Expected behavior: server rejects connection for both soft-fail values.
     *
     * @throws Exception
     */
    @Test
    public void testOcspRevokedResponderDownCrlActive() throws Exception {
        setCrl(CliUtils.asAbsolutePath(CA_BLANK_PEM_CRL));

        setSoftFail(false);
        testCommon(ocspCheckedRevokedKeyStore, trustStore, PASSWORD, true);

        setSoftFail(true);
        testCommon(ocspCheckedRevokedKeyStore, trustStore, PASSWORD, true);
    }

    /**
     * Let's check connection to server using an unknown certificate. OCSP responder is disabled; CRL not configured.
     * Expected behavior: server accepts connections for both soft-fail values.
     *
     * @throws Exception
     */
    @Test
    public void testOcspUnknownResponderDown() throws Exception {
        setCrl(null);

        setSoftFail(false);
        testCommon(ocspCheckedUnknownKeyStore, trustStore, PASSWORD, false);

        setSoftFail(true);
        testCommon(ocspCheckedUnknownKeyStore, trustStore, PASSWORD, true);
    }

    /**
     * Let's check connection to server using an unknown certificate. OCSP responder is disabled; CRL configured.
     * Expected behavior: server accepts connections for both soft-fail values.
     *
     * @throws Exception
     */
    @Test
    public void testOcspUnknownResponderDownCrlActive() throws Exception {
        setCrl(CliUtils.asAbsolutePath(CA_BLANK_PEM_CRL));

        setSoftFail(false);
        testCommon(ocspCheckedUnknownKeyStore, trustStore, PASSWORD, true);

        setSoftFail(true);
        testCommon(ocspCheckedUnknownKeyStore, trustStore, PASSWORD, true);
    }

    /**
     * This deployment is required just so OcspServerSetup task is executed.
     */
    @Deployment
    protected static WebArchive createDeployment() {
        return createDeployment("dummy");
    }

    @ArquillianResource
    protected URL url;

    protected static WebArchive createDeployment(final String name) {
        return ShrinkWrap.create(WebArchive.class, name + ".war");
    }
}
