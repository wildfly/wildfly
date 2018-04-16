/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.wildfly.extension.classchange;

import java.util.HashSet;
import java.util.Set;

import org.fakereplace.ReplaceableClassSelector;
import org.fakereplace.core.Fakereplace;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.Module;
import org.jboss.vfs.VirtualFile;

public class ClassChangeModuleDeploymentUnitProcessor implements DeploymentUnitProcessor {

    private static final AttachmentKey<ReplaceableClassSelector> SELECTOR = AttachmentKey.create(ReplaceableClassSelector.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit du = phaseContext.getDeploymentUnit();
        AbstractClassChangeSupport deploymentClassChangeSupport = (AbstractClassChangeSupport) du.getAttachment(ClassChangeAttachments.DEPLOYMENT_CLASS_CHANGE_SUPPORT);
        if (deploymentClassChangeSupport != null) {
            deploymentClassChangeSupport.doInitialScan();
            final Module module = du.getAttachment(Attachments.MODULE);
            ReplaceableClassSelector replaceableClassSelector = new ReplaceableClassSelector() {
                @Override
                public boolean isClassReplaceable(String s, ClassLoader classLoader) {
                    return classLoader == module.getClassLoader();
                }
            };
            Set<VirtualFile> roots = new HashSet<>();
            ResourceRoot rr = du.getAttachment(Attachments.DEPLOYMENT_ROOT);
            if(ModuleRootMarker.isModuleRoot(rr)) {
                roots.add(rr.getRoot());
            }
            for(ResourceRoot r : du.getAttachmentList(Attachments.RESOURCE_ROOTS)) {
                if(ModuleRootMarker.isModuleRoot(r)) {
                    roots.add(r.getRoot());
                }
            }

            deploymentClassChangeSupport.setClassLoader(module.getClassLoader());
            du.putAttachment(SELECTOR, replaceableClassSelector);
            Fakereplace.addReplaceableClassSelector(replaceableClassSelector);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        ReplaceableClassSelector selector = context.getAttachment(SELECTOR);
        if (selector != null) {
            Fakereplace.removeReplaceableClassSelector(selector);
        }
    }
}
