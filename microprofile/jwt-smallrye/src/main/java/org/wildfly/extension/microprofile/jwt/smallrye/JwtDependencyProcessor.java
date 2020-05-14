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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

/**
 * A {link {@link DeploymentUnitProcessor} to add the required dependencies to activate MicroProfile JWT.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class JwtDependencyProcessor implements DeploymentUnitProcessor {

    private static final String EE_SECURITY_API = "javax.security.enterprise.api";
    private static final String EE_SECURITY_IMPL = "org.glassfish.soteria";
    private static final String MP_JWT_API = "org.eclipse.microprofile.jwt.auth.api";
    private static final String SMALLRYE_JWT = "io.smallrye.jwt";
    private static final String ELYTRON_JWT = "org.wildfly.security.elytron-jwt";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!JwtDeploymentMarker.isJWTDeployment(deploymentUnit)) {
            return;
        }

        ModuleLoader moduleLoader = Module.getBootModuleLoader();
        ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpec.addSystemDependency(new ModuleDependency(moduleLoader, EE_SECURITY_API, false, false, true, false));
        moduleSpec.addSystemDependency(new ModuleDependency(moduleLoader, EE_SECURITY_IMPL, false, false, true, false));
        moduleSpec.addSystemDependency(new ModuleDependency(moduleLoader, MP_JWT_API, false, false, true, false));
        moduleSpec.addSystemDependency(new ModuleDependency(moduleLoader, SMALLRYE_JWT, false, false, true, false));
        moduleSpec.addSystemDependency(new ModuleDependency(moduleLoader, ELYTRON_JWT, false, false, true, false));
    }

    @Override
    public void undeploy(DeploymentUnit context) {}

}
