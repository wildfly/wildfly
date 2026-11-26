/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CSI.EstablishContext;
import org.omg.CSI.GSS_NT_ExportedNameHelper;
import org.omg.CSI.IdentityToken;
import org.omg.CSI.SASContextBody;
import org.omg.CSI.SASContextBodyHelper;
import org.omg.GSSUP.InitialContextToken;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.SecurityAttributeService;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.wildfly.iiop.openjdk.csiv2.CSIv2Util;
import org.wildfly.iiop.openjdk.csiv2.SASTargetInterceptor;

public class SASTargetInterceptorTestCase {

    private static ORB orb;
    private static Codec codec;

    @BeforeClass
    public static void setup() throws Exception {
        orb = ORB.init(new String[0], null);
        CodecFactory codecFactory = (CodecFactory) orb.resolve_initial_references("CodecFactory");
        Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value, (byte) 1, (byte) 2);
        codec = codecFactory.create_codec(encoding);
    }

    @After
    public void cleanup() {
        SASTargetInterceptor.setCallerPropagationRequireAuth(true);
    }

    @Test
    public void testValidGssupWithIdentityAssertionSucceeds() throws Exception {
        SASTargetInterceptor interceptor = new SASTargetInterceptor(codec);
        SASTargetInterceptor.setCallerPropagationRequireAuth(true);

        byte[] encodedContext = createSasContext("user1".getBytes(StandardCharsets.UTF_8),
                "password1".getBytes(StandardCharsets.UTF_8), "targetUser");

        ServerRequestInfo ri = createServerRequestInfo(encodedContext);
        interceptor.receive_request(ri);
    }

    @Test
    public void testEmptyUsernameInGssupWithIdentityAssertionIsRejected() throws Exception {
        SASTargetInterceptor interceptor = new SASTargetInterceptor(codec);
        SASTargetInterceptor.setCallerPropagationRequireAuth(true);

        byte[] encodedContext = createSasContext(new byte[0],
                "password1".getBytes(StandardCharsets.UTF_8), "targetUser");

        ServerRequestInfo ri = createServerRequestInfo(encodedContext);
        try {
            interceptor.receive_request(ri);
            Assert.fail("Expected NO_PERMISSION when client authentication token contains empty username");
        } catch (NO_PERMISSION expected) {
            // expected
        }
    }

    @Test
    public void testWhitespaceUsernameInGssupWithIdentityAssertionIsRejected() throws Exception {
        SASTargetInterceptor interceptor = new SASTargetInterceptor(codec);
        SASTargetInterceptor.setCallerPropagationRequireAuth(true);

        byte[] encodedContext = createSasContext("   ".getBytes(StandardCharsets.UTF_8),
                "password1".getBytes(StandardCharsets.UTF_8), "targetUser");

        ServerRequestInfo ri = createServerRequestInfo(encodedContext);
        try {
            interceptor.receive_request(ri);
            Assert.fail("Expected NO_PERMISSION when client authentication token contains whitespace username");
        } catch (NO_PERMISSION expected) {
            // expected
        }
    }

    @Test
    public void testNoAuthTokenWhenRequireAuthFalseSucceeds() throws Exception {
        SASTargetInterceptor interceptor = new SASTargetInterceptor(codec);
        SASTargetInterceptor.setCallerPropagationRequireAuth(false);

        byte[] encodedContext = createSasContext(null, null, "targetUser");

        ServerRequestInfo ri = createServerRequestInfo(encodedContext);
        interceptor.receive_request(ri);
    }

    private byte[] createSasContext(byte[] username, byte[] password, String principalName) throws Exception {
        byte[] clientAuthToken = new byte[0];
        if (username != null) {
            byte[] targetName = CSIv2Util.encodeGssExportedName("realm".getBytes(StandardCharsets.UTF_8));
            InitialContextToken initContextToken = new InitialContextToken(username, password, targetName);
            clientAuthToken = CSIv2Util.encodeInitialContextToken(initContextToken, codec);
        }

        IdentityToken identityToken = new IdentityToken();
        if (principalName != null) {
            byte[] exportedName = CSIv2Util.encodeGssExportedName(principalName.getBytes(StandardCharsets.UTF_8));
            Any anyName = orb.create_any();
            GSS_NT_ExportedNameHelper.insert(anyName, exportedName);
            byte[] encapsulatedPrincipalName = codec.encode_value(anyName);
            identityToken.principal_name(encapsulatedPrincipalName);
        } else {
            identityToken.absent(true);
        }

        EstablishContext establishContext = new EstablishContext(1L, new org.omg.CSI.AuthorizationElement[0], identityToken, clientAuthToken);
        SASContextBody sasContextBody = new SASContextBody();
        sasContextBody.establish_msg(establishContext);

        Any anyContext = orb.create_any();
        SASContextBodyHelper.insert(anyContext, sasContextBody);
        return codec.encode_value(anyContext);
    }

    private ServerRequestInfo createServerRequestInfo(final byte[] contextData) {
        return (ServerRequestInfo) Proxy.newProxyInstance(
                ServerRequestInfo.class.getClassLoader(),
                new Class<?>[]{ServerRequestInfo.class},
                (proxy, method, args) -> {
                    if ("get_request_service_context".equals(method.getName())) {
                        int id = (Integer) args[0];
                        if (id == SecurityAttributeService.value) {
                            return new ServiceContext(id, contextData);
                        }
                        throw new BAD_PARAM();
                    }
                    if ("operation".equals(method.getName())) {
                        return "testOp";
                    }
                    return null;
                });
    }
}
