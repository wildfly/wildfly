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
package org.jboss.as.controller;

import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * This implementation propagates properties present in the current
 * attribute value but missing from the new value.
 * Although, if the new value is of type UNDEFINED, the value
 * will remain UNDEFINED.
 *
 * @author Alexey Loubyansky
 */
public class PropagatingCorrector implements ParameterCorrector {

    public static final PropagatingCorrector INSTANCE = new PropagatingCorrector();

    /* (non-Javadoc)
     * @see org.jboss.as.controller.AttributeValueCorrector#correct(org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode)
     */
    @Override
    public ModelNode correct(ModelNode newValue, ModelNode currentValue) {
        if(newValue.getType() == ModelType.UNDEFINED) {
            return newValue;
        }
        if(newValue.getType() != ModelType.OBJECT || currentValue.getType() != ModelType.OBJECT) {
            return newValue;
        }
        final Set<String> operationKeys = newValue.keys();
        final Set<String> currentKeys = currentValue.keys();
        for(String currentKey : currentKeys) {
            if(!operationKeys.contains(currentKey)) {
                newValue.get(currentKey).set(currentValue.get(currentKey));
            }
        }
        return newValue;
    }
}
