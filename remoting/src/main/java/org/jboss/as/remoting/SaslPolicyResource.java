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
package org.jboss.as.remoting;

import static org.jboss.as.remoting.CommonAttributes.POLICY;
import static org.jboss.as.remoting.CommonAttributes.SASL_POLICY;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SaslPolicyResource extends SimpleResourceDefinition {
    static final PathElement SASL_POLICY_CONFIG_PATH = PathElement.pathElement(SASL_POLICY, POLICY);

    static final SaslPolicyResource INSTANCE = new SaslPolicyResource();

    static final SimpleAttributeDefinition FORWARD_SECRECY = createBooleanAttributeDefinition(CommonAttributes.FORWARD_SECRECY);
    static final SimpleAttributeDefinition NO_ACTIVE = createBooleanAttributeDefinition(CommonAttributes.NO_ACTIVE);
    static final SimpleAttributeDefinition NO_ANONYMOUS = createBooleanAttributeDefinition(CommonAttributes.NO_ANONYMOUS);
    static final SimpleAttributeDefinition NO_DICTIONARY = createBooleanAttributeDefinition(CommonAttributes.NO_DICTIONARY);
    static final SimpleAttributeDefinition NO_PLAIN_TEXT = createBooleanAttributeDefinition(CommonAttributes.NO_PLAIN_TEXT);
    static final SimpleAttributeDefinition PASS_CREDENTIALS = createBooleanAttributeDefinition(CommonAttributes.PASS_CREDENTIALS);

    private SaslPolicyResource() {
        super(SASL_POLICY_CONFIG_PATH,
                RemotingExtension.getResourceDescriptionResolver(POLICY),
                SaslPolicyAdd.INSTANCE,
                SaslPolicyRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        final OperationStepHandler writeHandler =
                new ReloadRequiredWriteAttributeHandler(FORWARD_SECRECY, NO_ACTIVE, NO_ANONYMOUS, NO_DICTIONARY,
                        NO_PLAIN_TEXT, PASS_CREDENTIALS);
        resourceRegistration.registerReadWriteAttribute(FORWARD_SECRECY, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(NO_ACTIVE, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(NO_ANONYMOUS, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(NO_DICTIONARY, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(NO_PLAIN_TEXT, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(PASS_CREDENTIALS, null, writeHandler);
    }

    private static SimpleAttributeDefinition createBooleanAttributeDefinition(String name) {
        return SimpleAttributeDefinitionBuilder.create(name, ModelType.BOOLEAN)
                .setDefaultValue(new ModelNode(true))
                .setAllowNull(true)
                .setAllowExpression(true)
                .setAttributeMarshaller(new WrappedAttributeMarshaller(Attribute.VALUE))
                .build();
    }
}
