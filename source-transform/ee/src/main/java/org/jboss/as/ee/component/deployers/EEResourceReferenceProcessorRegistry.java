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

package org.jboss.as.ee.component.deployers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.ee.logging.EeLogger;

/**
 * User: jpai
 */
public class EEResourceReferenceProcessorRegistry {

    private final Map<String, EEResourceReferenceProcessor> resourceReferenceProcessors = new ConcurrentHashMap<String, EEResourceReferenceProcessor>();

    public void registerResourceReferenceProcessor(final EEResourceReferenceProcessor resourceReferenceProcessor) {
        if (resourceReferenceProcessor == null) {
            throw EeLogger.ROOT_LOGGER.nullResourceReference();
        }
        final String resourceReferenceType = resourceReferenceProcessor.getResourceReferenceType();
        if (resourceReferenceType == null || resourceReferenceType.trim().isEmpty()) {
            throw EeLogger.ROOT_LOGGER.nullOrEmptyResourceReferenceType();
        }
        resourceReferenceProcessors.put(resourceReferenceType, resourceReferenceProcessor);
    }

    public EEResourceReferenceProcessor getResourceReferenceProcessor(final String resourceReferenceType) {
        return resourceReferenceProcessors.get(resourceReferenceType);
    }

}
