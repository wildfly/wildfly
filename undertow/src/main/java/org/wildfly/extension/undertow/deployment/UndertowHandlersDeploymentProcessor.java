package org.wildfly.extension.undertow.deployment;

import io.undertow.server.handlers.builder.PredicatedHandler;
import io.undertow.server.handlers.builder.PredicatedHandlersParser;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.Module;
import org.jboss.vfs.VirtualFile;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * DUP that handles undertow-handlers.conf
 *
 * @author Stuart Douglas
 */
public class UndertowHandlersDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String WEB_INF = "WEB-INF/undertow-handlers.conf";
    private static final String META_INF = "META-INF/undertow-handlers.conf";

    public static final AttachmentKey<List<PredicatedHandler>> PREDICATED_HANDLERS = AttachmentKey.create(List.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if(module == null) {
            return;
        }
        final List<PredicatedHandler> handlerWrappers = new ArrayList<>();
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            VirtualFile config = root.getRoot().getChild(WEB_INF);
            try {
                if (config.exists()) {
                    handlerWrappers.addAll(PredicatedHandlersParser.parse(config.openStream(), module.getClassLoader()));
                }
                Enumeration<URL> paths = module.getClassLoader().getResources(META_INF);
                while (paths.hasMoreElements()) {
                    URL path = paths.nextElement();
                    handlerWrappers.addAll(PredicatedHandlersParser.parse(path.openStream(), module.getClassLoader()));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if(!handlerWrappers.isEmpty()) {
                deploymentUnit.putAttachment(PREDICATED_HANDLERS, handlerWrappers);
            }

        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }

    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
