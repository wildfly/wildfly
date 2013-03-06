/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.model.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ModelTestOperationValidatorFilter implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean validateNone;
    private final List<OperationEntry> entries;

    private ModelTestOperationValidatorFilter(List<OperationEntry> entries) {
        this.entries = entries;
        validateNone = false;
    }

    private ModelTestOperationValidatorFilter(boolean validateNone) {
        this.validateNone = validateNone;
        entries = null;
    }

    public static ModelTestOperationValidatorFilter createValidateNone() {
        return new ModelTestOperationValidatorFilter(true);
    }

    public static ModelTestOperationValidatorFilter createValidateAll() {
        return new ModelTestOperationValidatorFilter(false);
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public ModelNode adjustForValidation(ModelNode op) {
        if (validateNone) {
            return null;
        } else if (entries == null) {
            return op;
        }

        //TODO handle composites

        ModelNode addr = op.get(OP_ADDR);
        PathAddress address = PathAddress.pathAddress(addr);

        String name = op.get(OP).asString();

        for (OperationEntry entry : entries) {
            if (nameMatch(name, entry)) {
                if (entry.address.size() == address.size()) {
                    for (int i = 0 ; i < address.size() ; i++) {
                        if (pathElementMatch(address.getElement(i), entry.address.getElement(i))) {
                            if (entry.action == Action.NOCHECK) {
                                return null;
                            } else {
                                return op.resolve();
                            }
                        }
                    }
                }
            }
        }
        return op;
    }

    private boolean nameMatch(String opName, OperationEntry entry) {
        if (entry.name.equals("*")) {
            return true;
        }
        return opName.equals(entry.name);
    }

    private boolean pathElementMatch(PathElement element, PathElement operationEntryElement) {
        if (operationEntryElement.getKey().equals("*")) {
        } else if (!operationEntryElement.getKey().equals(element.getKey())) {
            return false;
        }

        if (operationEntryElement.getValue().equals("*")) {
            return true;
        }
        return operationEntryElement.getValue().equals(element.getValue());
    }

    public static class Builder {
        List<OperationEntry> entries = new ArrayList<ModelTestOperationValidatorFilter.OperationEntry>();

        private Builder() {
        }

        public Builder addOperation(PathAddress pathAddress, String name, Action action) {
            entries.add(new OperationEntry(pathAddress, name, action));
            return this;
        }

        public ModelTestOperationValidatorFilter build() {
            return new ModelTestOperationValidatorFilter(entries);
        }
    }

    public static class OperationEntry implements Externalizable {
        private static final long serialVersionUID = 1L;
        private volatile PathAddress address;
        private volatile String name;
        private volatile Action action;

        public OperationEntry(PathAddress address, String name, Action action) {
            this.address = address;
            this.name = name;
            this.action = action;
        }

        public OperationEntry() {
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(name);
            out.writeObject(address.toModelNode());
            out.writeObject(action);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            name = (String)in.readObject();
            address = PathAddress.pathAddress((ModelNode)in.readObject());
            action = (Action)in.readObject();
        }
    }

    public static enum Action {
        NOCHECK,
        RESOLVE
    }

    public static void main(String[] args) {
        System.out.println(PathAddress.pathAddress(PathElement.pathElement("*", "*"), PathElement.pathElement("x", "*")));
    }
}
