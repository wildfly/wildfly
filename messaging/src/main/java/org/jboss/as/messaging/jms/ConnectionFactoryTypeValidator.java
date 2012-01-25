/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.messaging.CommonAttributes.GENERIC_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.QUEUE_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.TOPIC_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.XA_GENERIC_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.XA_QUEUE_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.XA_TOPIC_FACTORY;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

/**
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 *         Date: 1/24/12
 *         Time: 1:42 PM
 */
public class ConnectionFactoryTypeValidator extends ModelTypeValidator implements AllowedValuesValidator {

   public static ConnectionFactoryTypeValidator INSTANCE = new ConnectionFactoryTypeValidator();

   private static final List<ModelNode> ALLOWED_VALUES = new ArrayList<ModelNode>();

   static {
      ALLOWED_VALUES.add(new ModelNode().set(GENERIC_FACTORY));
      ALLOWED_VALUES.add(new ModelNode().set(QUEUE_FACTORY));
      ALLOWED_VALUES.add(new ModelNode().set(TOPIC_FACTORY));
      ALLOWED_VALUES.add(new ModelNode().set(XA_GENERIC_FACTORY));
      ALLOWED_VALUES.add(new ModelNode().set(XA_QUEUE_FACTORY));
      ALLOWED_VALUES.add(new ModelNode().set(XA_TOPIC_FACTORY));
   }

   public ConnectionFactoryTypeValidator() {
      super(ModelType.STRING, true);
   }

   public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined() && value.getType() != ModelType.EXPRESSION) {
            if(!ALLOWED_VALUES.contains(value)) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidParameterValue(value.asString(), parameterName, ALLOWED_VALUES)));
            }
        }
    }

    public List<ModelNode> getAllowedValues() {
        return ALLOWED_VALUES;
    }
}