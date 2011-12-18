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

package org.jboss.as.remoting;


import static org.xnio.Options.SSL_ENABLED;
import static org.xnio.Options.SSL_STARTTLS;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * @author Jaikiran Pai
 */
abstract class AbstractOutboundConnectionAddHandler extends AbstractAddStepHandler {

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        AbstractOutboundConnectionResourceDefinition.CONNECTION_CREATION_OPTIONS.validateAndSet(operation, model);
    }

    /**
     * Parses the {@link CommonAttributes#CONNECTION_CREATION_OPTIONS} from the passed <code>model</code>
     * and returns a {@link OptionMap} representing those options. If the <code>model</code> doesn't
     * have any {@link CommonAttributes#CONNECTION_CREATION_OPTIONS} then this method returns {@link OptionMap#EMPTY}
     *
     * @param model The model which might contain the connection creation options
     * @return
     */
    protected static OptionMap getConnectionCreationOptions(final ModelNode model) {
        if (!model.hasDefined(CommonAttributes.CONNECTION_CREATION_OPTIONS)) {
            return OptionMap.create(SSL_ENABLED, true, SSL_STARTTLS, true);
        }
        final OptionMap.Builder optionMapBuilder = OptionMap.builder();
        final List<Property> connectionCreationProps = model.get(CommonAttributes.CONNECTION_CREATION_OPTIONS).asPropertyList();
        for (final Property property : connectionCreationProps) {
            final String xnioOptionName = property.getName();
            final String value = property.getValue().asString();

            final String fullyQualifiedOptionName = Options.class.getName() + "." + xnioOptionName;
            // create the XNIO option for the option name
            final Option connectionCreationOption = Option.fromString(fullyQualifiedOptionName, Options.class.getClassLoader());
            // now parse and set the value
            optionMapBuilder.parse(connectionCreationOption, value);
        }
        return optionMapBuilder.getMap();
    }
}
