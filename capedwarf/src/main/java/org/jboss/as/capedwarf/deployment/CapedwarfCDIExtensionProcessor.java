/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.metadata.MetadataImpl;

import javax.enterprise.inject.spi.Extension;
import java.lang.reflect.Constructor;

/**
 * Enable CDI for CapeDwarf module.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfCDIExtensionProcessor extends CapedwarfWebDeploymentProcessor {

    private static final String[] EMPTY_STRING_ARRAY = {};
    private static final String[] EXTENSIONS = {"org.jboss.capedwarf.admin.AdminExtension"};

    @Override
    protected void doDeploy(DeploymentUnit unit) throws DeploymentUnitProcessingException {
        final DeploymentReflectionIndex index = unit.getAttachment(Attachments.REFLECTION_INDEX);
        final ClassLoader cl = unit.getAttachment(Attachments.MODULE).getClassLoader();
        for (String fqn : EXTENSIONS) {
            final Extension extension = loadExtension(fqn, index, cl);
            final Metadata<Extension> metadata = new MetadataImpl<Extension>(extension, unit.getName());
            log.debug("Loaded portable extension " + extension);
            unit.addToAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS, metadata);
        }
    }

    @SuppressWarnings("unchecked")
    private Extension loadExtension(final String serviceClassName, final DeploymentReflectionIndex index, final ClassLoader loader) throws DeploymentUnitProcessingException {
        try {
            final Class<Extension> serviceClass = (Class<Extension>) loader.loadClass(serviceClassName);
            final Constructor<Extension> ctor = index.getClassIndex(serviceClass).getConstructor(EMPTY_STRING_ARRAY);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException("Cannot load CDI Extension.", e);
        }
    }
}
