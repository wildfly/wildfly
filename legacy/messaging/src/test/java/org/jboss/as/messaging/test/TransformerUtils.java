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

package org.jboss.as.messaging.test;

import static org.jboss.as.messaging.CommonAttributes.PARAM;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.model.test.FailedOperationTransformationConfig;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class TransformerUtils {

    static String[] concat(AttributeDefinition[] attrs1, String... attrs2) {
        String[] newAttrs = new String[attrs1.length + attrs2.length];
        for(int i = 0; i < attrs1.length; i++) {
            newAttrs[i] = attrs1[i].getName();
        }
        for(int i = 0; i < attrs2.length; i++) {
            newAttrs[attrs1.length + i] = attrs2[i];
        }
        return newAttrs;
    }

    static AttributeDefinition[] concat(AttributeDefinition[] attrs1, AttributeDefinition... attrs2) {
        AttributeDefinition[] newAttrs = new AttributeDefinition[attrs1.length + attrs2.length];
        for(int i = 0; i < attrs1.length; i++) {
            newAttrs[i] = attrs1[i];
        }
        for(int i = 0; i < attrs2.length; i++) {
            newAttrs[attrs1.length + i] = attrs2[i];
        }
        return newAttrs;
    }

    static FailedOperationTransformationConfig.ChainedConfig createChainedConfig(AttributeDefinition[] rejectedExpression, AttributeDefinition[] newAttributes) {
        AttributeDefinition[] allAttributes = new AttributeDefinition[rejectedExpression.length + newAttributes.length];
        System.arraycopy(rejectedExpression, 0, allAttributes, 0, rejectedExpression.length);
        System.arraycopy(newAttributes, 0, allAttributes, rejectedExpression.length, newAttributes.length);

        return FailedOperationTransformationConfig.ChainedConfig.createBuilder(allAttributes)
            .addConfig(new FailedOperationTransformationConfig.RejectExpressionsConfig(rejectedExpression))
            .addConfig(new FailedOperationTransformationConfig.NewAttributesConfig(newAttributes)).build();
    }

    static class RejectExpressionsConfigWithAddOnlyParam extends FailedOperationTransformationConfig.RejectExpressionsConfig {

        RejectExpressionsConfigWithAddOnlyParam(String... attributes) {
            super(attributes);
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            if (PARAM.equals(attributeName)) {
                return false;
            }
            return super.isAttributeWritable(attributeName);
        }
    }
}
