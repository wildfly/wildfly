/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.remoting.SaslPolicyResource.FORWARD_SECRECY;
import static org.jboss.as.remoting.SaslPolicyResource.NO_ACTIVE;
import static org.jboss.as.remoting.SaslPolicyResource.NO_ANONYMOUS;
import static org.jboss.as.remoting.SaslPolicyResource.NO_DICTIONARY;
import static org.jboss.as.remoting.SaslPolicyResource.NO_PLAIN_TEXT;
import static org.jboss.as.remoting.SaslPolicyResource.PASS_CREDENTIALS;
import static org.jboss.as.remoting.SaslPolicyResource.SASL_POLICY_CONFIG_PATH;

import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;

/**
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat, inc
 */
public class SaslPolicyResourceTransformers {

    static TransformersSubRegistration registerTransformers(TransformersSubRegistration parent) {
        TransformersSubRegistration policy = parent.registerSubResource(SASL_POLICY_CONFIG_PATH);
        RejectExpressionValuesTransformer rejectExpression = new RejectExpressionValuesTransformer(FORWARD_SECRECY,
                NO_ACTIVE, NO_ANONYMOUS, NO_DICTIONARY, NO_PLAIN_TEXT, PASS_CREDENTIALS);
        policy.registerOperationTransformer(ADD, rejectExpression);
        policy.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, rejectExpression.getWriteAttributeTransformer());

        return policy;
    }
}
