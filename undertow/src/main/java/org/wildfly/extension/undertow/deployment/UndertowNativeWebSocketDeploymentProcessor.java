package org.wildfly.extension.undertow.deployment;

import java.util.ArrayList;

import io.undertow.servlet.websockets.WebSocketServlet;
import io.undertow.websockets.api.WebSocketSessionHandler;
import io.undertow.websockets.core.handler.WebSocketConnectionCallback;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.DeploymentClassIndex;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;

/**
 * Deployment processor for native (not JSR) web sockets.
 * <p/>
 * If a {@link WebSocketSessionHandler} or {@link WebSocketSessionHandler}is mapped as a servlet then it is replaced by the
 * handshake servlet
 *
 * @author Stuart Douglas
 */
public class UndertowNativeWebSocketDeploymentProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentClassIndex classIndex = deploymentUnit.getAttachment(Attachments.CLASS_INDEX);
        WarMetaData metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (metaData == null) {
            return;
        }
        JBossWebMetaData mergedMetaData = metaData.getMergedJBossWebMetaData();

        if (mergedMetaData.getServlets() != null) {
            for (final JBossServletMetaData servlet : mergedMetaData.getServlets()) {
                if (servlet.getServletClass() != null) {
                    try {
                        Class<?> clazz = classIndex.classIndex(servlet.getServletClass()).getModuleClass();
                        if (WebSocketSessionHandler.class.isAssignableFrom(clazz) ||
                                WebSocketConnectionCallback.class.isAssignableFrom(clazz)) {
                            servlet.setServletClass(WebSocketServlet.class.getName());
                            if (servlet.getInitParam() == null) {
                                servlet.setInitParam(new ArrayList<ParamValueMetaData>());
                            }
                            final ParamValueMetaData param = new ParamValueMetaData();
                            param.setParamName(WebSocketServlet.SESSION_HANDLER);
                            param.setParamValue(clazz.getName());
                            servlet.getInitParam().add(param);
                        }
                    } catch (ClassNotFoundException e) {
                        throw new DeploymentUnitProcessingException(e);
                    }
                }
            }
        }


    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
