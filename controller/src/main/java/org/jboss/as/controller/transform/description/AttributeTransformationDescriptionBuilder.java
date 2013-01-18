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



/**
 * @author Emanuel Muckenhuber
 * @author kabir
 */
public interface AttributeTransformationDescriptionBuilder<T> {

    /**
     * Make this attribute reject expressions
     *
     * @return this builder
     */
    AttributeTransformationDescriptionBuilder<T> setRejectExpressions(T...attributes);

    AttributeTransformationDescriptionBuilder<T> addRejectCheck(RejectAttributeChecker rejectChecker, T...rejectedAttributes);

    AttributeTransformationDescriptionBuilder<T> addRejectChecks(List<RejectAttributeChecker> rejectCheckers, T...rejectedAttributes);

    AttributeTransformationDescriptionBuilder<T> setDiscard(DiscardAttributeChecker discardChecker, T...discardedAttributes);

    AttributeTransformationDescriptionBuilder<T> addRename(T attributeName, String newName);

    AttributeTransformationDescriptionBuilder<T> addRenames(Map<T, String> renames);

    AttributeTransformationDescriptionBuilder<T> setValueConverter(AttributeConverter attributeConverter, T...convertedAttributes);

    AttributeTransformationDescriptionBuilder<T> addAttribute(T attribute, AttributeConverter attributeConverter);

    /**
     * Finish with this attribute builder and return control to the parent resource transformation builder
     *
     * @return the parent builder
     */
    ResourceTransformationDescriptionBuilder end();
}
