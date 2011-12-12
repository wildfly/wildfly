/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.descriptions;

import java.util.Locale;
import java.util.Map;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Provides description elements to override the description of a resource produced by a {@link DescriptionProvider}.
 * For use with specifically named resources (i.e. those whose {@link ManagementResourceRegistration} path is identified
 * with a {@link PathElement#pathElement(String, String) two-argument PathElement}) that expose additional attributes or
 * operations not provided by the generic resource description (i.e. the {@link ManagementResourceRegistration} whose
 * path is identified with a {@link PathElement#pathElement(String) one-argument PathElement}.)
 *
 * @see ManagementResourceRegistration#registerOverrideModel(String, OverrideDescriptionProvider)
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface OverrideDescriptionProvider {

    /**
     * Provides descriptions for attributes that are in addition to those provided by the generic resource.
     *
     * @param locale locale to use for generating internationalized descriptions
     *
     * @return map whose keys are attribute names and whose values are the descriptions of the attribute to
     *         incorporate into the overall resource description.
     */
    Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale);

    /**
     * Provides descriptions for child types that are in addition to those provided by the generic resource.
     *
     * @param locale locale to use for generating internationalized descriptions
     *
     * @return map whose keys are child type names and whose values are the descriptions of the child type to
     *         incorporate into the overall resource description.
     */
    Map<String, ModelNode> getChildTypeOverrideDescriptions(Locale locale);
}
