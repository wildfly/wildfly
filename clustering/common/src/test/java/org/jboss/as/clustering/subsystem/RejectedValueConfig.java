/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.subsystem;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.model.test.FailedOperationTransformationConfig.AttributesPathAddressConfig;
import org.jboss.dmr.ModelNode;

/**
 * Generic {@link AttributesPathAddressConfig} for a rejected attribute.
 * @author Paul Ferraro
 */
public class RejectedValueConfig extends AttributesPathAddressConfig<RejectedValueConfig> {

    private final Predicate<ModelNode> rejection;
    private final UnaryOperator<ModelNode> corrector;

    public RejectedValueConfig(Attribute attribute, Predicate<ModelNode> rejection) {
        this(attribute, rejection, value -> attribute.getDefinition().getDefaultValue());
    }

    public RejectedValueConfig(Attribute attribute, Predicate<ModelNode> rejection, UnaryOperator<ModelNode> corrector) {
        super(attribute.getDefinition().getName());
        this.rejection = rejection;
        this.corrector = corrector;
    }

    @Override
    protected boolean isAttributeWritable(String attributeName) {
        return true;
    }

    @Override
    protected boolean checkValue(String attrName, ModelNode attribute, boolean isGeneratedWriteAttribute) {
        return this.rejection.test(attribute);
    }

    @Override
    protected ModelNode correctValue(ModelNode toResolve, boolean isGeneratedWriteAttribute) {
        return this.corrector.apply(toResolve);
    }
}
