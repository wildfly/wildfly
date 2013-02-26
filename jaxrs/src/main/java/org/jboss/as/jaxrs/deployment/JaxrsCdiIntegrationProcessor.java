/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.jaxrs.deployment;

import static org.jboss.as.jaxrs.JaxrsLogger.JAXRS_LOGGER;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.resteasy.cdi.ResteasyCdiExtension;
import org.jboss.weld.bootstrap.spi.Metadata;

import javax.enterprise.inject.spi.Extension;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class JaxrsCdiIntegrationProcessor implements DeploymentUnitProcessor {

    public static final String CDI_INJECTOR_FACTORY_CLASS = "org.jboss.resteasy.cdi.CdiInjectorFactory";

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        if (!JaxrsDeploymentMarker.isJaxrsDeployment(deploymentUnit)) {
            return;
        }

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return;
        }

        final DeploymentUnit parent = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        final JBossWebMetaData webdata = warMetaData.getMergedJBossWebMetaData();

        try {
            module.getClassLoader().loadClass(CDI_INJECTOR_FACTORY_CLASS);
            // don't set this param if CDI is not in classpath
            if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                JAXRS_LOGGER.debug("Found CDI, adding injector factory class");
                setContextParameter(webdata, "resteasy.injector.factory", CDI_INJECTOR_FACTORY_CLASS);
                //now we need to add the CDI extension, if it has not
                //already been added
                synchronized (parent) {
                    boolean found = false;
                    final List<Metadata<Extension>> extensions = parent.getAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS);
                    for (Metadata<Extension> extension : extensions) {
                        if (extension.getValue() instanceof ResteasyCdiExtension) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {

                        final ClassLoader classLoader = SecurityActions.getContextClassLoader();
                        try {
                            //MASSIVE HACK
                            //the resteasy Logger throws a NPE if the TCCL is null
                            SecurityActions.setContextClassLoader(ResteasyCdiExtension.class.getClassLoader());
                            final ResteasyCdiExtension ext = new ResteasyCdiExtension();
                            Metadata<Extension> metadata = new Metadata<Extension>() {
                                @Override
                                public Extension getValue() {
                                    return ext;
                                }

                                @Override
                                public String getLocation() {
                                    return "org.jboss.as.jaxrs.JaxrsExtension";
                                }
                            };
                            parent.addToAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS, metadata);
                        } finally {
                            SecurityActions.setContextClassLoader(classLoader);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
    }


    public static void setContextParameter(JBossWebMetaData webdata, String name, String value) {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(name);
        param.setParamValue(value);
        List<ParamValueMetaData> params = webdata.getContextParams();
        if (params == null) {
            params = new ArrayList<ParamValueMetaData>();
            webdata.setContextParams(params);
        }
        params.add(param);
    }
    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
