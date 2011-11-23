/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

import java.io.IOException;

/**
 * Add CapeDwarf modules.
 *
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfDeploymentProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier APPENGINE = ModuleIdentifier.create("com.google.appengine");
    private static final ModuleIdentifier CAPEDWARF = ModuleIdentifier.create("org.jboss.capedwarf");

    private final VirtualFileFilter LIBS = new VirtualFileFilter() {
        @Override
        public boolean accepts(VirtualFile file) {
            return file.getName().endsWith(".jar");
        }
    };

    /**
     * The relative order of this processor within the phase.
     * The current number is large enough for it to happen after all
     * the standard deployment unit processors that come with JBoss AS.
     */
    public static final int PRIORITY = 0x4000;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();

        if (CapedwarfDeploymentMarker.isCapedwarfDeployment(unit) == false)
            return; // Skip non CapeDwarf deployments

        ModuleSpecification moduleSpecification = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        // always add CapeDwarf
        moduleSpecification.addSystemDependency(createModuleDependency(CAPEDWARF));
         // check if we bundle gae api jar
        if (hasAppEngineAPI(unit)) {
            moduleSpecification.addClassFileTransformer("org.jboss.capedwarf.bytecode.FactoriesTransformer");
        } else {
            moduleSpecification.addSystemDependency(createModuleDependency(APPENGINE));
        }
    }

    protected boolean hasAppEngineAPI(DeploymentUnit unit) throws DeploymentUnitProcessingException {
        try {
            final ResourceRoot root = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            VirtualFile libs = root.getRoot().getChild("WEB-INF/lib");
            if (libs.exists()) {
                for (VirtualFile lib : libs.getChildren(LIBS)) {
                    if (lib.getName().contains("appengine-api"))
                        return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    protected ModuleDependency createModuleDependency(ModuleIdentifier moduleIdentifier) {
        return new ModuleDependency(Module.getBootModuleLoader(), moduleIdentifier, false, false, true);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
