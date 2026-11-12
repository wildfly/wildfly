/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.compression;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that the {@link org.jboss.ejb.client.annotation.CompressionHint} on remote view classes and the view methods is taken into account during EJB invocations
 *
 * @author: Jaikiran Pai
 * @see https://issues.jboss.org/browse/EJBCLIENT-76
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class CompressionTestCase {

    private static final String MODULE_NAME = "ejb-invocation-compression-test";

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(CompressionTestCase.class.getPackage());
        return jar;
    }

    /**
     * Tests that EJB invocations that are marked with a compression hint can be invoked without any problems. This test doesn't actually poke in to verify if the data
     * was actually compressed, because that's supposed to be transparent to the bean. All this test does is to make sure that when the hint is in place, the invocations can pass.
     *
     * @throws Exception
     */
    @Test
    public void testCompressedInvocation() throws Exception {
        // set the system property which enables annotation scan on the client view
        System.setProperty("org.jboss.ejb.client.view.annotation.scan.enabled", "true");
        try {
            // create a proxy for invocation
            final Properties props = new Properties();
            props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            final Context jndiCtx = new InitialContext(props);
            final MethodOverrideDataCompressionRemoteView bean = (MethodOverrideDataCompressionRemoteView) jndiCtx.lookup("ejb:" + "" + "/" + MODULE_NAME + "/" + "" + "/" + CompressableDataBean.class.getSimpleName() + "!" + MethodOverrideDataCompressionRemoteView.class.getName());
            final String message = "some message";

            // only request compression
            final String echoWithRequestCompressed = bean.echoWithRequestCompress(message);
            Assertions.assertEquals(message, echoWithRequestCompressed, "Unexpected response for invocation with only request compressed");

            // only response compressed
            final String echoWithResponseCompressed = bean.echoWithResponseCompress(message);
            Assertions.assertEquals(message, echoWithResponseCompressed, "Unexpected response for invocation with only response compressed");

            // both request and response compressed based on the annotation at the view class level
            final String echoWithRequestAndResponseCompressed = bean.echoWithNoExplicitDataCompressionHintOnMethod(message);
            Assertions.assertEquals(message, echoWithRequestAndResponseCompressed, "Unexpected response for invocation with both request and response compressed");

        } finally {
            // remove the system property which enables annotation scan on the client view
            System.getProperties().remove("org.jboss.ejb.client.view.annotation.scan.enabled");
        }
    }
}
