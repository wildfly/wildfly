/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.microprofile.jwt.smallrye;

import static org.wildfly.extension.microprofile.jwt.smallrye._private.MicroProfileJWTLogger.ROOT_LOGGER;

import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.security.VirtualDomainMarkerUtility;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;

/**
 * A {@link DeploymentUnitProcessor} to detect if MicroProfile JWT should be activated.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class JwtActivationProcessor implements DeploymentUnitProcessor {

    private static final String AUTH_METHOD = "authMethod";
    private static final String REALM_NAME = "realmName";
    private static final String JWT_AUTH_METHOD = "MP-JWT";

    private static final DotName APPLICATION_DOT_NAME = DotName.createSimple("javax.ws.rs.core.Application");
    private static final DotName LOGIN_CONFIG_DOT_NAME = DotName.createSimple("org.eclipse.microprofile.auth.LoginConfig");


    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            return;
        }

        JBossWebMetaData mergedMetaData = warMetaData.getMergedJBossWebMetaData();
        LoginConfigMetaData loginConfig = mergedMetaData != null ? mergedMetaData.getLoginConfig() : null;
        if (loginConfig != null && !JWT_AUTH_METHOD.equals(loginConfig.getAuthMethod())) {
            // An auth-method has been defined, this is not MP-JWT
            return;
        }

        if (loginConfig == null) {
            final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
            List<AnnotationInstance> annotations = index.getAnnotations(LOGIN_CONFIG_DOT_NAME);
            for (AnnotationInstance annotation : annotations) {
                // First we must be sure the annotation is on an Application class.
                AnnotationTarget target = annotation.target();
                if (target.kind() == Kind.CLASS) {
                    if (extendsApplication(target.asClass(), index)) {
                        loginConfig = new LoginConfigMetaData();
                        AnnotationValue authMethodValue = annotation.value(AUTH_METHOD);
                        if (authMethodValue == null) {
                            throw ROOT_LOGGER.noAuthMethodSpecified();
                        }
                        loginConfig.setAuthMethod(authMethodValue.asString());
                        AnnotationValue realmNameValue = annotation.value(REALM_NAME);
                        if (realmNameValue != null) {
                            loginConfig.setRealmName(realmNameValue.asString());
                        }

                        mergedMetaData.setLoginConfig(loginConfig);

                        break;
                    }
                }
                ROOT_LOGGER.loginConfigInvalidTarget(target.toString());
            }
        }

        if (loginConfig != null && JWT_AUTH_METHOD.equals(loginConfig.getAuthMethod())) {
            ROOT_LOGGER.tracef("Activating JWT for deployment %s.", deploymentUnit.getName());
            JwtDeploymentMarker.mark(deploymentUnit);
            VirtualDomainMarkerUtility.virtualDomainRequired(deploymentUnit);
        }

    }

    private boolean extendsApplication(ClassInfo classInfo, CompositeIndex index) {
        if (classInfo == null) {
            return false;
        }

        DotName superType = classInfo.superName();

        if (superType == null) {
            return false;
        } else if (APPLICATION_DOT_NAME.equals(superType)) {
            return true;
        }

        return extendsApplication(index.getClassByName(superType), index);
    }

    @Override
    public void undeploy(DeploymentUnit context) {}

}
