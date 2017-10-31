/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.deployment;

import org.junit.Assert;
import org.junit.Test;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import io.undertow.servlet.api.AuthMethodConfig;

public class AuthMethodParserTest {

    /**
     *
     * Test for checking that auth method parser doesn't decode twice the auth method query parameter
     */
    @Test
    public void testPEMEncoded() throws Exception {
        String pemOrig = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQClH5+52mqHLdChbOfzuyue5FSDl2n1mOkpMlF1676NT79AScHVMi1Io"
                + "hWkuSe3W+oPLE+GAwyyr0DyolUmTkrhrMID6LamgmH8IzhOeyaxDOjwbCIUeGM1V9Qht+nTneRMhGa/oL687XioZiE1Ev52D8kMa"
                + "KMNMHprL9oOZ/QM4wIDAQAB";
        String pemEnc = URLEncoder.encode(pemOrig, "UTF-8");
        HashMap<String, String> props = new HashMap<>();
        List<AuthMethodConfig> authMethodConfigs = AuthMethodParser.parse("CUSTOM?publicKey="+pemEnc, props);
        AuthMethodConfig authMethodConfig = authMethodConfigs.get(0);
        String pemDecode = authMethodConfig.getProperties().get("publicKey");
        Assert.assertEquals("publicKey = pemOrig; failed probably due https://issues.jboss.org/browse/WFLY-9135",
                pemOrig, pemDecode);
    }
}
