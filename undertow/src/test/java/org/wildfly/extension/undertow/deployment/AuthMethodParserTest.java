/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
