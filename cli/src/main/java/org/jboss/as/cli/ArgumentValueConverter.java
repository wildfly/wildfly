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
package org.jboss.as.cli;


import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public interface ArgumentValueConverter {

    abstract class DMRWithFallbackConverter implements ArgumentValueConverter {
        @Override
        public ModelNode fromString(String value) throws CommandFormatException {
            if(value == null) {
                return new ModelNode();
            }
            try {
                return ModelNode.fromString(value);
            } catch(Exception e) {
                return fromNonDMRString(value);
            }
        }

        protected abstract ModelNode fromNonDMRString(String value) throws CommandFormatException;
    }

    ArgumentValueConverter DEFAULT = new ArgumentValueConverter() {
        @Override
        public ModelNode fromString(String value) throws CommandFormatException {
            if (value == null) {
                return new ModelNode();
            }
            ModelNode toSet = null;
            try {
                toSet = ModelNode.fromString(value);
            } catch (Exception e) {
                // just use the string
                toSet = new ModelNode().set(value);
            }
            return toSet;
        }
    };

    ArgumentValueConverter LIST = new DMRWithFallbackConverter() {
        @Override
        protected ModelNode fromNonDMRString(String value)
                throws CommandFormatException {
            final ModelNode list = new ModelNode();
            for (String item : value.split(",")) {
                list.add(new ModelNode().set(item));
            }
            return list;
        }
    };

    ArgumentValueConverter PROPERTIES = new DMRWithFallbackConverter() {
        @Override
        protected ModelNode fromNonDMRString(String value)
                throws CommandFormatException {
            final String[] props = value.split(",");
            final ModelNode list = new ModelNode();
            for (String prop : props) {
                int equals = prop.indexOf('=');
                if (equals == -1) {
                    throw new CommandFormatException("Property '" + prop + "' in '" + value + "' is missing the equals sign.");
                }
                String propName = prop.substring(0, equals);
                if (propName.isEmpty()) {
                    throw new CommandFormatException("Property name is missing for '" + prop + "' in '" + value + "'");
                }
                list.add(propName, prop.substring(equals + 1));
            }
            return list;
        }
    };

    ModelNode fromString(String value) throws CommandFormatException;
}
