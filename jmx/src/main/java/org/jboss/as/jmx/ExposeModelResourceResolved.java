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
package org.jboss.as.jmx;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * The resource for the legacy behaviour where all attributes are read as resolved and
 * attributes/operation parameters are typed and you can't use expressions
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ExposeModelResourceResolved extends ExposeModelResource {

    static final SimpleAttributeDefinition DOMAIN_NAME = SimpleAttributeDefinitionBuilder.create(CommonAttributes.DOMAIN_NAME, ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(CommonAttributes.DEFAULT_RESOLVED_DOMAIN))
            .build();

    static final SimpleAttributeDefinition PROPER_PROPERTY_FORMAT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PROPER_PROPERTY_FORMAT, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(true))
            .build();

    static final ExposeModelResourceResolved INSTANCE = new ExposeModelResourceResolved();

    private ExposeModelResourceResolved() {
        super(CommonAttributes.RESOLVED, DOMAIN_NAME, PROPER_PROPERTY_FORMAT);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadWriteAttribute(PROPER_PROPERTY_FORMAT, null, new JMXWriteAttributeHandler(PROPER_PROPERTY_FORMAT));
    }


}
