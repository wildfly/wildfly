package org.jboss.as.test.integration.ejb.compression;


import org.jboss.ejb.client.annotation.CompressionHint;

/**
 * @author: Jaikiran Pai
 */
@CompressionHint
public interface MethodOverrideDataCompressionRemoteView {

    @CompressionHint(compressRequest = false)
    String echoWithResponseCompress(String msg);

    @CompressionHint(compressResponse = false)
    String echoWithRequestCompress(String msg);

    String echoWithNoExplicitDataCompressionHintOnMethod(String msg);
}
