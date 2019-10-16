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

package org.wildfly.test.integration.elytron.certs.ocsp;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CliUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.elytron.util.WelcomeContent;

/**
 * Tests to check OCSP feature using XiPKI OCSP responder.
 *
 * @author Jan Stourac <jstourac@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({OcspTestBase.OcspServerSetup.class, WelcomeContent.SetupTask.class})
public class OcspTestCase extends OcspTestBase {

    /**
     * Let's check connection to server using a valid certificate. OCSP responder is active; CRL not configured.
     * Expected behavior: server accepts connection for both soft-fail values.
     *
     * @throws Exception
     */
    @Test
    public void testOcspGood() throws Exception {
        setCrl(null);
        setSoftFail(false);
        testCommon(ocspCheckedGoodKeyStore, trustStore, PASSWORD, true);

        setSoftFail(true);
        testCommon(ocspCheckedGoodKeyStore, trustStore, PASSWORD, true);
    }

    /**
     * Let's check connection to server using a valid certificate. OCSP responder is active; CRL configured.
     * Expected behavior: server accepts connection for both soft-fail values.
     *
     * @throws Exception
     */
    @Test
    public void testOcspGoodCrlActive() throws Exception {
        setCrl(CliUtils.asAbsolutePath(CA_BLANK_PEM_CRL));
        setSoftFail(false);
        testCommon(ocspCheckedGoodKeyStore, trustStore, PASSWORD, true);

        setSoftFail(true);
        testCommon(ocspCheckedGoodKeyStore, trustStore, PASSWORD, true);
    }

    /**
     * Let's check connection to server using a revoked certificate. OCSP responder is enabled; CRL not configured.
     * Expected behavior: server rejects connection for both soft-fail values.
     *
     * @throws Exception
     */
    @Test
    public void testOcspRevoked() throws Exception {
        setCrl(null);
        setSoftFail(false);
        testCommon(ocspCheckedRevokedKeyStore, trustStore, PASSWORD, false);

        setSoftFail(true);
        testCommon(ocspCheckedRevokedKeyStore, trustStore, PASSWORD, false);
    }

    /**
     * Let's check connection to server using a revoked certificate. OCSP responder is enabled; CRL configured.
     * Expected behavior: server rejects connection for both soft-fail values.
     *
     * @throws Exception
     */
    @Test
    public void testOcspRevokedCrlActive() throws Exception {
        setCrl(CliUtils.asAbsolutePath(CA_BLANK_PEM_CRL));
        setSoftFail(false);
        testCommon(ocspCheckedRevokedKeyStore, trustStore, PASSWORD, false);

        setSoftFail(true);
        testCommon(ocspCheckedRevokedKeyStore, trustStore, PASSWORD, false);
    }

    /**
     * Let's check connection to server using an unknown certificate. OCSP responder is enabled; CRL not configured.
     * Expected behavior: server rejects connection for soft-fail=false but accepts connection for soft-fail=true.
     *
     * @throws Exception
     */
    @Test
    public void testOcspUnknown() throws Exception {
        setCrl(null);
        setSoftFail(false);
        testCommon(ocspCheckedUnknownKeyStore, trustStore, PASSWORD, false);

        setSoftFail(true);
        testCommon(ocspCheckedUnknownKeyStore, trustStore, PASSWORD, true);
    }

    /**
     * Let's check connection to server using an unknown certificate. OCSP responder is enabled; CRL configured.
     * Expected behavior: server rejects connection for soft-fail=false but accepts connection for soft-fail=true.
     *
     * @throws Exception
     */
    @Test
    public void testOcspUnknownCrlActive() throws Exception {
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
