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

import static org.jboss.as.remoting.CommonAttributes.FORWARD_SECRECY;
import static org.jboss.as.remoting.CommonAttributes.NO_ACTIVE;
import static org.jboss.as.remoting.CommonAttributes.NO_ANONYMOUS;
import static org.jboss.as.remoting.CommonAttributes.NO_DICTIONARY;
import static org.jboss.as.remoting.CommonAttributes.NO_PLAIN_TEXT;
import static org.jboss.as.remoting.CommonAttributes.PASS_CREDENTIALS;
import static org.jboss.as.remoting.CommonAttributes.POLICY;
import static org.jboss.as.remoting.CommonAttributes.SASL_POLICY;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
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

    static final AttributeDefinition FORWARD_SECRECY_ATTRIBUTE = new BooleanValueAttributeDefinition(FORWARD_SECRECY);
    static final AttributeDefinition NO_ACTIVE_ATTRIBUTE = new BooleanValueAttributeDefinition(NO_ACTIVE);
    static final AttributeDefinition NO_ANONYMOUS_ATTRIBUTE = new BooleanValueAttributeDefinition(NO_ANONYMOUS);
    static final AttributeDefinition NO_DICTIONARY_ATTRIBUTE = new BooleanValueAttributeDefinition(NO_DICTIONARY);
    static final AttributeDefinition NO_PLAIN_TEXT_ATTRIBUTE = new BooleanValueAttributeDefinition(NO_PLAIN_TEXT);
    static final AttributeDefinition PASS_CREDENTIALS_ATTRIBUTE = new BooleanValueAttributeDefinition(PASS_CREDENTIALS);

    private SaslPolicyResource() {
        super(SASL_POLICY_CONFIG_PATH,
                RemotingExtension.getResourceDescriptionResolver(POLICY),
                SaslPolicyAdd.INSTANCE,
                SaslPolicyRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(FORWARD_SECRECY_ATTRIBUTE, null);
        resourceRegistration.registerReadOnlyAttribute(NO_ACTIVE_ATTRIBUTE, null);
        resourceRegistration.registerReadOnlyAttribute(NO_ANONYMOUS_ATTRIBUTE, null);
        resourceRegistration.registerReadOnlyAttribute(NO_DICTIONARY_ATTRIBUTE, null);
        resourceRegistration.registerReadOnlyAttribute(NO_PLAIN_TEXT_ATTRIBUTE, null);
        resourceRegistration.registerReadOnlyAttribute(PASS_CREDENTIALS_ATTRIBUTE, null);
    }

    private static class BooleanValueAttributeDefinition extends NamedValueAttributeDefinition {
        public BooleanValueAttributeDefinition(String name) {
            super(name, Attribute.VALUE, new ModelNode().set(true), ModelType.BOOLEAN, true);
        }
    }
}
