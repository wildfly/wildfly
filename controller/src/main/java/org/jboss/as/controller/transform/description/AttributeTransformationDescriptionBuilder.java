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
 * Builder for the attribute transformer.
 *
 * @author Emanuel Muckenhuber
 * @author kabir
 */
public interface AttributeTransformationDescriptionBuilder {

    /**
     * Adds a RejectAttributeChecker. The RejectAttributeCheckers are processed in the order they are added to the passed in attributes.
     * Rejection is done after the attribute has been checked for discarding.
     *
     * @param rejectChecker the checker
     * @param rejectedAttributes the attributes to check
     * @return this builder
     */
    AttributeTransformationDescriptionBuilder addRejectCheck(RejectAttributeChecker rejectChecker, String...rejectedAttributes);

    /**
     * Adds a RejectAttributeChecker. The RejectAttributeCheckers are processed in the order they are added to the passed in attributes.
     * Rejection is done after the attribute has been checked for discarding.
     *
     * @param rejectChecker the checker
     * @param rejectedAttributes the attributes to check
     * @return this builder
     */
    AttributeTransformationDescriptionBuilder addRejectCheck(RejectAttributeChecker rejectChecker, AttributeDefinition...rejectedAttributes);

    /**
     * Adds a list of RejectAttributeCheckers. The RejectAttributeCheckers are processed in the order they are added to the passed in attributes.
     * Rejection is done after the attribute has been checked for discarding.
     *
     * @param rejectCheckers the checkers
     * @param rejectedAttributes the attributes to check
     * @return this builder
     */
    AttributeTransformationDescriptionBuilder addRejectChecks(List<RejectAttributeChecker> rejectCheckers, String...rejectedAttributes);

    /**
     * Adds a list of RejectAttributeCheckers. The RejectAttributeCheckers are processed in the order they are added to the passed in attributes.
     * Rejection is done after the attribute has been checked for discarding.
     *
     * @param rejectCheckers the checkers
     * @param rejectedAttributes the attributes to check
     * @return this builder
     */
    AttributeTransformationDescriptionBuilder addRejectChecks(List<RejectAttributeChecker> rejectCheckers, AttributeDefinition...rejectedAttributes);

    /**
     * Sets the DiscardChecker.
     *
     * @param discardChecker the checkers
     * @param discardedAttributes the attributes to check
     * @return this builder
     */
    AttributeTransformationDescriptionBuilder setDiscard(DiscardAttributeChecker discardChecker, String...discardedAttributes);

    /**
     * Sets the DiscardChecker.
     *
     * @param discardChecker the checkers
     * @param discardedAttributes the attributes to check
     * @return this builder
     */
    AttributeTransformationDescriptionBuilder setDiscard(DiscardAttributeChecker discardChecker, AttributeDefinition...discardedAttributes);

    /**
     * Use to rename an attribute. This is done after all attributes have been discarded, checked for rejection and converted.
     *
     * @param attributeName the attribute's original name
     * @param newName the new name for the attribute
     * @return this builder
     */
    AttributeTransformationDescriptionBuilder addRename(String attributeName, String newName);

    /**
     * Use to rename an attribute. This is done after all attributes have been discarded, checked for rejection and converted.
     *
     * @param attributeName the attribute's original name
     * @param newName the new name for the attribute
     * @return this builder
     */
    AttributeTransformationDescriptionBuilder addRename(AttributeDefinition attributeName, String newName);

    /**
     * Use to rename attribute. This is done after all attributes have been discarded, checked for rejection and converted.
     *
     * @param renames a Map where the keys are the original attribute names, and the values are the new attribute names
     * @return this builder
     */
    AttributeTransformationDescriptionBuilder addRenames(Map<String, String> renames);

    /**
     * Use to convert an attribute's value. This is done after the attribute has been checked for discarding and checked for rejection.
     *
     * @param attributeConverter the attribute converter used to convert the value of an attribute
     * @param convertedAttributes the attributes the attribute converter should be used on
     * @return this builder
     */
    AttributeTransformationDescriptionBuilder setValueConverter(AttributeConverter attributeConverter, String...convertedAttributes);

    /**
     * Use to convert an attribute's value. This is done after the attribute has been checked for discarding and checked for rejection.
     * If it is a new attribute, it is added after the attribute has been checked for discarding and checked for rejection.
     *
     * @param attributeConverter the attribute converter used to convert the value of an attribute
     * @param convertedAttributes the attributes the attribute converter should be used on
     * @return this builder
     */
    AttributeTransformationDescriptionBuilder setValueConverter(AttributeConverter attributeConverter, AttributeDefinition...convertedAttributes);

    /**
     * Finish with this attribute builder and return control to the parent resource transformation builder
     *
     * @return the parent builder
     */
    ResourceTransformationDescriptionBuilder end();
}
