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
import org.jboss.as.controller.transform.ResourceTransformer;



/**
 * Builder for the attribute transformer for a resource/operation. The transformer created by this interface executes the following phases in the following order:
 *
 * <li>
 * <ul>{@code DISCARD} - All attributes with a {@link DiscardAttributeChecker} registered are checked to see if the attribute should be discarded</ul>
 * <ul>{@code REJECT} - All attributes with a {@link RejectAttributeChecker}s registered are checked to see if the attribute should be rejected</ul>
 * <ul>{@code CONVERT} - All attributes with a {@link AttributeConverter} registered are checked to see if the attribute should be converted. If the attribute does not
 *      exist in the original operation/resource the {@link AttributeConverter} may register a new attribute</ul>
 * <ul>{@code RENAME} - All attributes with a rename registered are renamed</ul>
 * </li>
 *
 * All attributes are processed in each phase before moving onto the next one.  See the individual methods for information about how to add rules for each phase.
 * The {@link ResourceTransformer} which may be registered for a resource in {@link ResourceTransformationDescriptionBuilder#setCustomResourceTransformer(ResourceTransformer)}
 * is executed after all  the conversions done by this builder.
 *
 * @see ResourceTransformationDescriptionBuilder#getAttributeBuilder()
 * @see ResourceTransformationDescriptionBuilder#setCustomResourceTransformer(ResourceTransformer)
 *
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public interface BaseAttributeTransformationDescriptionBuilder<T extends BaseAttributeTransformationDescriptionBuilder<?>> {

    /**
     * Adds a RejectAttributeChecker. More than one reject checker can be used for an attribute, and the RejectAttributeCheckers
     * are processed in the order they are added to the passed in attributes.
     * <p>
     * Rejection is done in the {@code REJECT} phase.
     *
     * @param rejectChecker the checker
     * @param rejectedAttributes the attributes to check
     * @return this builder
     */
    T addRejectCheck(RejectAttributeChecker rejectChecker, String...rejectedAttributes);

    /**
     * Adds a RejectAttributeChecker. More than one reject checker can be used for an attribute, and the RejectAttributeCheckers
     * are processed in the order they are added to the passed in attributes.
     * <p>
     * Rejection is done in the {@code REJECT} phase.
     *
     * @param rejectChecker the checker
     * @param rejectedAttributes the attributes to check
     * @return this builder
     */
    T addRejectCheck(RejectAttributeChecker rejectChecker, AttributeDefinition...rejectedAttributes);

    /**
     * Adds a list of RejectAttributeCheckers. More than one reject checker can be used for an attribute, and the RejectAttributeCheckers
     * are processed in the order they are added to the passed in attributes.
     * <p>
     * Rejection is done in the {@code REJECT} phase.
     *
     * @param rejectCheckers the checkers
     * @param rejectedAttributes the attributes to check
     * @return this builder
     */
    T addRejectChecks(List<RejectAttributeChecker> rejectCheckers, String...rejectedAttributes);

    /**
     * Adds a list of RejectAttributeCheckers. More than one reject checker can be used for an attribute, and the RejectAttributeCheckers
     * are processed in the order they are added to the passed in attributes.
     * <p>
     * Rejection is done in the {@code REJECT} phase.
     *
     * @param rejectCheckers the checkers
     * @param rejectedAttributes the attributes to check
     * @return this builder
     */
    T addRejectChecks(List<RejectAttributeChecker> rejectCheckers, AttributeDefinition...rejectedAttributes);

    /**
     * Sets the DiscardChecker to be used to check if an attribute should be discarded. Only one discard checker can be used
     * for an attribute.
     * <p>
     * Discard is done in the {@code DISCARD} phase.
     *
     * @param discardChecker the checkers
     * @param discardedAttributes the attributes to check
     * @return this builder
     */
    T setDiscard(DiscardAttributeChecker discardChecker, String...discardedAttributes);

    /**
     * Sets the DiscardChecker to be used to check if an attribute should be discarded. Only one discard checker can be used
     * for an attribute.
     * <p>
     * Discard is done in the {@code DISCARD} phase.
     *
     * @param discardChecker the checkers
     * @param discardedAttributes the attributes to check
     * @return this builder
     */
    T setDiscard(DiscardAttributeChecker discardChecker, AttributeDefinition...discardedAttributes);

    /**
     * Rename an attribute. An attribute can only be renamed once.
     * <p>
     * Renaming is done in the {@code RENAME} phase.
     *
     * @param attributeName the attribute's original name
     * @param newName the new name for the attribute
     * @return this builder
     */
    T addRename(String attributeName, String newName);

    /**
     * Rename an attribute. An attribute can only be renamed once.
     * <p>
     * Renaming is done in the {@code RENAME} phase.
     *
     * @param attributeName the attribute's original name
     * @param newName the new name for the attribute
     * @return this builder
     */
    T addRename(AttributeDefinition attributeName, String newName);

    /**
     * Rename attributes. Each attribute can only be renamed once.
     * <p>
     * Renaming is done in the {@code RENAME} phase.
     *
     * @param renames a Map where the keys are the original attribute names, and the values are the new attribute names
     * @return this builder
     */
    T addRenames(Map<String, String> renames);

    /**
     * Use to convert an attribute's value. If it is a new attribute that did not exist in the original resource/operation,
     * it is added. Only one AttributeConverter can be added per attribute.
     * <p>
     * Conversion/Adding is done in the {@code CONVERT} phase.
     *
     * @param attributeConverter the attribute converter used to convert the value of each attribute
     * @param convertedAttributes the attributes the attribute converter should be used on
     * @return this builder
     */
    T setValueConverter(AttributeConverter attributeConverter, String...convertedAttributes);

    /**
     * Use to convert an attribute's value. If it is a new attribute that did not exist in the original resource/operation,
     * it is added. Only one AttributeConverter can be added per attribute.
     * <p>
     * Conversion/Adding is done in the {@code CONVERT} phase.
     *
     * @param attributeConverter the attribute converter used to convert the value of each attribute
     * @param convertedAttributes the attributes the attribute converter should be used on
     * @return this builder
     */
    T setValueConverter(AttributeConverter attributeConverter, AttributeDefinition...convertedAttributes);

    /**
     * Finish with this attribute builder and return control to the parent resource transformation builder
     *
     * @return the parent builder
     */
    ResourceTransformationDescriptionBuilder end();
}
