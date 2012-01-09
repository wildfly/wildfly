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
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.VFSResourceLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ResourceLoader;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Add CapeDwarf modules.
 *
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfDeploymentProcessor extends CapedwarfDeploymentUnitProcessor {

    private static final ModuleIdentifier APPENGINE = ModuleIdentifier.create("com.google.appengine");
    private static final ModuleIdentifier CAPEDWARF = ModuleIdentifier.create("org.jboss.capedwarf");
    private static final ModuleIdentifier INFINISPAN = ModuleIdentifier.create("org.infinispan");

    private static final ModuleIdentifier TX = ModuleIdentifier.create("javax.transaction.api");
    private static final ModuleIdentifier ACTIVATION = ModuleIdentifier.create("javax.activation.api");
    private static final ModuleIdentifier MAIL = ModuleIdentifier.create("org.javassist");
    private static final ModuleIdentifier JAVASSIST = ModuleIdentifier.create("javax.mail.api");
    private static final ModuleIdentifier INFINISPAN_QUERY = ModuleIdentifier.create("org.infinispan.query");
    private static final ModuleIdentifier HIBERNATE_SEARCH = ModuleIdentifier.create("org.hibernate.search");
    private static final ModuleIdentifier LUCENE = ModuleIdentifier.create("org.apache.lucene");
    private static final ModuleIdentifier HTTP_COMPONENTS = ModuleIdentifier.create("org.apache.httpcomponents");
    private static final ModuleIdentifier PICKETLINK = ModuleIdentifier.create("org.picketlink.fed");
    // inline this module deps, if running with bundled
    private static final ModuleIdentifier[] INLINE = {TX, ACTIVATION, MAIL, JAVASSIST, INFINISPAN_QUERY, HIBERNATE_SEARCH, LUCENE, HTTP_COMPONENTS, PICKETLINK};

    private static final VirtualFileFilter JARS = new VirtualFileFilter() {
        @Override
        public boolean accepts(VirtualFile file) {
            return file.getName().endsWith(".jar");
        }
    };

    private List<ResourceLoaderSpec> capedwarfResources;

    private String appengingAPI;

    public CapedwarfDeploymentProcessor(String appengingAPI) {
        if (appengingAPI == null)
            appengingAPI = "appengine-api";
        this.appengingAPI = appengingAPI;
    }

    @Override
    protected void doDeploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();

        final ModuleSpecification moduleSpecification = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        // always add Infinispan
        moduleSpecification.addSystemDependency(createModuleDependency(INFINISPAN));
        // check if we bundle gae api jar
        if (hasAppEngineAPI(unit)) {
            // add a transformer, modifying GAE service factories
            moduleSpecification.addClassFileTransformer("org.jboss.capedwarf.bytecode.FactoriesTransformer");
            // add CapeDwarf resources directly as libs
            for (ResourceLoaderSpec rls : getCapedwarfResources())
                moduleSpecification.addResourceLoader(rls);
            // add other needed dependencies
            for (ModuleIdentifier mi : INLINE)
                moduleSpecification.addSystemDependency(createModuleDependency(mi));
        } else {
            // add CapeDwarf
            moduleSpecification.addSystemDependency(createModuleDependency(CAPEDWARF));
            // add modified AppEngine
            moduleSpecification.addSystemDependency(createModuleDependency(APPENGINE));
        }
    }

    protected boolean hasAppEngineAPI(DeploymentUnit unit) throws DeploymentUnitProcessingException {
        try {
            final ResourceRoot root = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            final VirtualFile libs = root.getRoot().getChild("WEB-INF/lib");
            if (libs.exists()) {
                for (VirtualFile lib : libs.getChildren(JARS)) {
                    if (lib.getName().contains(appengingAPI))
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

    protected synchronized List<ResourceLoaderSpec> getCapedwarfResources() throws DeploymentUnitProcessingException {
        try {
            if (capedwarfResources == null) {
                // hardcoded location of CapeDwarf resources ...
                final URI capedwarfModules = new File(System.getProperty("jboss.home.dir"), "modules/org/jboss/capedwarf/main").toURI();
                final VirtualFile vf = VFS.getChild(capedwarfModules);
                if (vf.exists() == false)
                    throw new DeploymentUnitProcessingException("No such CapeDwarf modules directory: " + capedwarfModules);

                final List<ResourceLoaderSpec> resources = new ArrayList<ResourceLoaderSpec>();
                for (VirtualFile jar : vf.getChildren(JARS)) {
                    ResourceLoader rl = new VFSResourceLoader(jar.getName(), jar);
                    resources.add(ResourceLoaderSpec.createResourceLoaderSpec(rl));
                }
                capedwarfResources = resources;
            }
            return capedwarfResources;
        } catch (DeploymentUnitProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }
}
