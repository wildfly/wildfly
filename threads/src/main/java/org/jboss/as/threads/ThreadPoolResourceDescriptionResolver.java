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

package org.jboss.as.threads;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;

/**
 * {@link StandardResourceDescriptionResolver} variant that reuses a set of common attribute descriptions
 * for various pool resource types.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class ThreadPoolResourceDescriptionResolver extends StandardResourceDescriptionResolver {

    private static final Set<String> COMMON_ATTRIBUTE_NAMES;

    static {
        COMMON_ATTRIBUTE_NAMES = new HashSet<String>(Arrays.asList(PoolAttributeDefinitions.NAME.getName(),
                PoolAttributeDefinitions.ALLOW_CORE_TIMEOUT.getName(), PoolAttributeDefinitions.CORE_THREADS.getName(),
                PoolAttributeDefinitions.HANDOFF_EXECUTOR.getName(), PoolAttributeDefinitions.KEEPALIVE_TIME.getName(),
                PoolAttributeDefinitions.MAX_THREADS.getName(), PoolAttributeDefinitions.QUEUE_LENGTH.getName(),
                PoolAttributeDefinitions.THREAD_FACTORY.getName(), PoolAttributeDefinitions.ACTIVE_COUNT.getName(),
                PoolAttributeDefinitions.COMPLETED_TASK_COUNT.getName(), PoolAttributeDefinitions.CURRENT_THREAD_COUNT.getName(),
                PoolAttributeDefinitions.LARGEST_THREAD_COUNT.getName(), PoolAttributeDefinitions.TASK_COUNT.getName()));

        // note we don't include REJECTED_COUNT as it has a different definition in different resources
    }

    private static final String COMMON_PREFIX = "threadpool.common";

    ThreadPoolResourceDescriptionResolver(final String keyPrefix, final String bundleBaseName, final ClassLoader bundleLoader) {
        super(keyPrefix, bundleBaseName, bundleLoader, true, true);
    }

    @Override
    public String getResourceAttributeDescription(String attributeName, Locale locale, ResourceBundle bundle) {
        if (COMMON_ATTRIBUTE_NAMES.contains(attributeName)) {
            return bundle.getString(getBundleKey(attributeName));
        }
        return super.getResourceAttributeDescription(attributeName, locale, bundle);
    }

    @Override
    public String getResourceAttributeValueTypeDescription(String attributeName, Locale locale, ResourceBundle bundle, String... suffixes) {
        if (COMMON_ATTRIBUTE_NAMES.contains(attributeName)) {
            return bundle.getString(getVariableBundleKey(new String[] {attributeName}, suffixes));
        }
        return super.getResourceAttributeValueTypeDescription(attributeName, locale, bundle, suffixes);
    }

    @Override
    public String getOperationParameterDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
        if (ModelDescriptionConstants.ADD.equals(operationName) && COMMON_ATTRIBUTE_NAMES.contains(paramName)) {
            return bundle.getString(getBundleKey(paramName));
        }
        return super.getOperationParameterDescription(operationName, paramName, locale, bundle);
    }

    @Override
    public String getOperationParameterValueTypeDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle, String... suffixes) {
        if (ModelDescriptionConstants.ADD.equals(operationName) && COMMON_ATTRIBUTE_NAMES.contains(paramName)) {
            return bundle.getString(getVariableBundleKey(new String[] {paramName}, suffixes));
        }
        return super.getOperationParameterValueTypeDescription(operationName, paramName, locale, bundle, suffixes);
    }


    private String getBundleKey(String... args) {
        return getVariableBundleKey(args);
    }

    private String getVariableBundleKey(String[] fixed, String... variable) {
        StringBuilder sb = new StringBuilder(COMMON_PREFIX);
        for (String arg : fixed) {
            sb.append('.');
            sb.append(arg);
        }
        if (variable != null) {
            for (String arg : variable) {
                sb.append('.');
                sb.append(arg);
            }
        }
        return sb.toString();
    }
}
