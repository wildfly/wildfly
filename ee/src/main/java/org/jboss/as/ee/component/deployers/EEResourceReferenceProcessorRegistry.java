/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
