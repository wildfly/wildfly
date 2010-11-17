/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web.deployment;

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.getVirtualFileAttachment;
import static org.jboss.as.deployment.module.ModuleDependencies.getAttachedDependencies;

import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.module.ModuleConfig;
import org.jboss.as.deployment.module.ModuleConfig.ResourceRoot;
import org.jboss.as.deployment.module.ModuleDependencies;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import static org.jboss.as.web.deployment.WarDeploymentMarker.isWarDeployment;
import org.jboss.as.web.deployment.helpers.DeploymentStructure;
import org.jboss.as.web.deployment.helpers.DeploymentStructure.ClassPathEntry;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

/**
 * War {@code ModuleConfig} processor.
 *
 * @author Emanuel Muckenhuber
 */
public class WarModuleConfigProcessor implements DeploymentUnitProcessor {

    public static final String WEB_INF_LIB = "WEB-INF/lib";
    public static final String WEB_INF_CLASSES = "WEB-INF/classes";

    public static final VirtualFileFilter DEFAULT_WEB_INF_LIB_FILTER = new SuffixMatchFilter(".jar", VisitorAttributes.DEFAULT);

    public static final long PRIORITY = DeploymentPhases.MODULARIZE.plus(100L);
    private static final ModuleConfig.Dependency[] NO_DEPS = new ModuleConfig.Dependency[0];

    /**
     * Create the {@code ModuelConfig} for a .war deployment
     *
     * {@inheritDoc}
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        if(!isWarDeployment(context)) {
            return; // Skip non web deployments
        }
        if(context.getAttachment(ModuleConfig.ATTACHMENT_KEY) != null) {
            return;
        }
        final VirtualFile deploymentRoot = getVirtualFileAttachment(context);
        final ModuleIdentifier moduleIdentifier = ModuleIdentifier.create("deployment." + deploymentRoot.getName());
        final ModuleConfig.ResourceRoot[] resourceRoots = createResourceRoots(context.getAttachment(DeploymentStructure.ATTACHMENT_KEY));
        final ModuleDependencies dependenciesAttachment = getAttachedDependencies(context);
        final ModuleConfig.Dependency[] dependencies = dependenciesAttachment != null ? dependenciesAttachment.getDependencies() : NO_DEPS;
        final ModuleConfig moduleConfig = new ModuleConfig(moduleIdentifier, dependencies, resourceRoots);
        context.putAttachment(ModuleConfig.ATTACHMENT_KEY, moduleConfig);
    }

    private ResourceRoot[] createResourceRoots(final DeploymentStructure structure) {
        if(structure == null) {
            return new ResourceRoot[0];
        }
        final ClassPathEntry[] entries = structure.getEntries();
        if(entries == null || entries.length == 0) {
            return new ResourceRoot[0];
        }
        final int length = entries.length;
        final ResourceRoot[] roots = new ResourceRoot[length];
        for(int i = 0; i < length; i++) {
            final ClassPathEntry entry = entries[i];
            roots[i] = new ModuleConfig.ResourceRoot(entry.getName(), entry.getRoot(), entry.getMountHandle());
        }
        return roots;
    }



}
