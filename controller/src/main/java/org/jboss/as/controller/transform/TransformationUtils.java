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

package org.jboss.as.controller.transform;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.LegacyResourceDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
class TransformationUtils {

    private TransformationUtils() {
        //
    }

    public static ModelNode getSubsystemDefinitionForVersion(final String subsystemName, ModelVersion version) {

        StringBuilder key = new StringBuilder(subsystemName).append("-").append(version.getMajor()).append(".").append(version.getMinor());
        if(version.getMicro()!=0){
            key.append('.').append(version.getMicro());
        }
        key.append(".dmr");
        InputStream is = null;
        try {
            is = TransformerRegistry.class.getResourceAsStream(key.toString());
            if (is == null) {
                return null;
            }
            return ModelNode.fromStream(is);
        } catch (IOException e) {
            ControllerLogger.ROOT_LOGGER.cannotReadTargetDefinition(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    //
                }
            }
        }
        return null;
    }

    public static ResourceDefinition loadSubsystemDefinition(final String subsystemName, ModelVersion version) {
        final ModelNode desc = getSubsystemDefinitionForVersion(subsystemName, version);
        if (desc == null) {
            return null;
        }
        return new LegacyResourceDefinition(desc);
    }

    public static Resource modelToResource(final ImmutableManagementResourceRegistration reg, final ModelNode model) {
        return modelToResource(reg, model, false);
    }

    public static Resource modelToResource(final ImmutableManagementResourceRegistration reg, final ModelNode model, boolean includeUndefined) {
        Resource res = Resource.Factory.create();
        ModelNode value = new ModelNode();
        for (String name : reg.getAttributeNames(PathAddress.EMPTY_ADDRESS)) {
            //todo we need to handle cases where there is data on original model but attributes are not on IMRR
            if (includeUndefined) {
                value.get(name).set(model.get(name));
            } else {
                if (model.hasDefined(name)) {
                    value.get(name).set(model.get(name));
                }
            }
        }
        if (!value.isDefined() && model.isDefined() && reg.getChildAddresses(PathAddress.EMPTY_ADDRESS).size() == 0) {
            value.setEmptyObject();
        }
        res.writeModel(value);

        for (PathElement path : reg.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {

            ImmutableManagementResourceRegistration sub = reg.getSubModel(PathAddress.pathAddress(path));
            if (path.isWildcard()) {
                ModelNode subModel = model.get(path.getKey());
                if (subModel.isDefined()) {
                    for (Property p : subModel.asPropertyList()) {
                        if (p.getValue().isDefined()) {
                            res.registerChild(PathElement.pathElement(path.getKey(), p.getName()), modelToResource(sub, p.getValue(), includeUndefined));
                        }
                    }
                }
            } else {
                ModelNode subModel = model.get(path.getKeyValuePair());
                if (subModel.isDefined()) {
                    res.registerChild(path, modelToResource(sub, subModel));
                }
            }
        }
        return res;
    }

}
