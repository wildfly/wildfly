package org.jboss.as.test.integration.ejb.compression;


import org.jboss.ejb.client.annotation.CompressionHint;

/**
 * @author: Jaikiran Pai
 */
@CompressionHint
public interface MethodOverrideDataCompressionRemoteView {

    @CompressionHint(compressRequest = false)
    String echoWithResponseCompress(final String msg);

    @CompressionHint(compressResponse = false)
    String echoWithRequestCompress(final String msg);

    String echoWithNoExplicitDataCompressionHintOnMethod(String msg);
}
