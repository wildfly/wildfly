package org.jboss.as.ejb3.remote.protocol.versiontwo;

import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.RemoteAsyncInvocationCancelStatusService;
import org.jboss.as.ejb3.remote.protocol.versionone.MethodInvocationMessageHandler;
import org.jboss.ejb.client.annotation.CompressionHint;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.remoting3.MessageOutputStream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Modified MethodInvocationMessageHandler allowing response compression.
 *
 * @author <a href="mailto:thofman@redhat.com">Tomas Hofman</a>
 */
public class CompressedMethodInvocationMessageHandler extends MethodInvocationMessageHandler {

    private static final byte HEADER_COMPRESSED_MESSAGE = 0x1B;

    CompressedMethodInvocationMessageHandler(DeploymentRepository deploymentRepository, MarshallerFactory marshallerFactory, ExecutorService executorService, RemoteAsyncInvocationCancelStatusService asyncInvocationCancelStatus) {
        super(deploymentRepository, marshallerFactory, executorService, asyncInvocationCancelStatus);
    }


    protected DataOutputStream wrapMessageOutputStream(MessageOutputStream messageOutputStream, Method invokedMethod) throws IOException {
        // look for CompressionHint annotation
        // first method level
        CompressionHint compressionHint = invokedMethod.getAnnotation(CompressionHint.class);
        // then class level
        if (compressionHint == null) {
            compressionHint = invokedMethod.getDeclaringClass().getAnnotation(CompressionHint.class);
        }

        // if the compression hint is set, compress the response data
        if (compressionHint != null && compressionHint.compressResponse()) {
            final int compressionLevel = compressionHint.compressionLevel();
            // write out the header indicating that it's a compressed stream
            messageOutputStream.write(HEADER_COMPRESSED_MESSAGE);
            // create the deflater using the specified level
            final Deflater deflater = new Deflater(compressionLevel);
            // wrap the message outputstream with the deflater stream so that *any subsequent* data writes to the stream are compressed
            final DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(messageOutputStream, deflater);
            if (EjbLogger.EJB3_INVOCATION_LOGGER.isTraceEnabled()) {
                EjbLogger.EJB3_INVOCATION_LOGGER.trace("Using a compressing stream with compression level = " + compressionLevel + " for response data for EJB invocation on method " + invokedMethod);
            }
            return new DataOutputStream(deflaterOutputStream);
        }

        // no CompressionHint applicable for this invocation
        return new DataOutputStream(messageOutputStream);
    }

}
