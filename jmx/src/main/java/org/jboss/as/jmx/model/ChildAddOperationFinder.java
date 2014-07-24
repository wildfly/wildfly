/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jmx.model;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ChildAddOperationFinder {

    static Map<PathElement, ChildAddOperationEntry> findAddChildOperations(ImmutableManagementResourceRegistration resourceRegistration){
        Map<PathElement, ChildAddOperationEntry> operations = new HashMap<PathElement, ChildAddOperationEntry>();
        for(PathElement childElement : resourceRegistration.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {
            final ImmutableManagementResourceRegistration childReg = resourceRegistration.getSubModel(PathAddress.pathAddress(childElement));
            final Map<String, OperationEntry> registeredOps = childReg.getOperationDescriptions(PathAddress.EMPTY_ADDRESS, false);
            final OperationEntry childAdd = registeredOps.get(ADD);
            if (childAdd != null) {
                operations.put(childElement, new ChildAddOperationEntry(childAdd, childElement));
            }
        }
        return operations;
    }

    static ChildAddOperationEntry findAddChildOperation(ImmutableManagementResourceRegistration resourceRegistration, String addName){
        for(PathElement childElement : resourceRegistration.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {
            final ImmutableManagementResourceRegistration childReg = resourceRegistration.getSubModel(PathAddress.pathAddress(childElement));
            final Map<String, OperationEntry> registeredOps = childReg.getOperationDescriptions(PathAddress.EMPTY_ADDRESS, false);
            final OperationEntry childAdd = registeredOps.get(ADD);
            if (childAdd != null) {
                if (NameConverter.createValidAddOperationName(childElement).equals(addName)) {
                    return new ChildAddOperationEntry(childAdd, childElement);
                }
            }
        }
        return null;
    }

    static class ChildAddOperationEntry {
        private final OperationEntry op;
        private final PathElement element;

        public ChildAddOperationEntry(OperationEntry op, PathElement element) {
            this.op = op;
            this.element = element;
        }

        public OperationEntry getOperationEntry() {
            return op;
        }

        public PathElement getElement() {
            return element;
        }
    }
}
