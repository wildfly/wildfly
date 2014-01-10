/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import io.undertow.servlet.websockets.WebSocketServlet;
import io.undertow.websockets.WebSocketConnectionCallback;
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

import java.util.ArrayList;

/**
 * Deployment processor for native (not JSR) web sockets.
 * <p/>
 * If a {@link WebSocketConnectionCallback} is mapped as a servlet then it is replaced by the
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
                        if (WebSocketConnectionCallback.class.isAssignableFrom(clazz)) {
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
