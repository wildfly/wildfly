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

package org.jboss.as.controller.transform.description;

import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;



/**
 * @author Emanuel Muckenhuber
 * @author kabir
 */
public interface AttributeTransformationDescriptionBuilder {

    AttributeTransformationDescriptionBuilder addRejectCheck(RejectAttributeChecker rejectChecker, String...rejectedAttributes);
    AttributeTransformationDescriptionBuilder addRejectCheck(RejectAttributeChecker rejectChecker, AttributeDefinition...rejectedAttributes);

    AttributeTransformationDescriptionBuilder addRejectChecks(List<RejectAttributeChecker> rejectCheckers, String...rejectedAttributes);
    AttributeTransformationDescriptionBuilder addRejectChecks(List<RejectAttributeChecker> rejectCheckers, AttributeDefinition...rejectedAttributes);

    AttributeTransformationDescriptionBuilder setDiscard(DiscardAttributeChecker discardChecker, String...discardedAttributes);
    AttributeTransformationDescriptionBuilder setDiscard(DiscardAttributeChecker discardChecker, AttributeDefinition...discardedAttributes);

    AttributeTransformationDescriptionBuilder addRename(String attributeName, String newName);
    AttributeTransformationDescriptionBuilder addRename(AttributeDefinition attributeName, String newName);

    AttributeTransformationDescriptionBuilder addRenames(Map<String, String> renames);
    // AttributeTransformationDescriptionBuilder<T> addRenames(Map<AttributeDefinition, String> renames);

    AttributeTransformationDescriptionBuilder setValueConverter(AttributeConverter attributeConverter, String...convertedAttributes);
    AttributeTransformationDescriptionBuilder setValueConverter(AttributeConverter attributeConverter, AttributeDefinition...convertedAttributes);

    AttributeTransformationDescriptionBuilder addAttribute(String attribute, AttributeConverter attributeConverter);
    AttributeTransformationDescriptionBuilder addAttribute(AttributeDefinition attribute, AttributeConverter attributeConverter);

    /**
     * Finish with this attribute builder and return control to the parent resource transformation builder
     *
     * @return the parent builder
     */
    ResourceTransformationDescriptionBuilder end();
}
