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
import java.util.ResourceBundle;

/**
 * Resolves localized text descriptions of resources and their components.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ResourceDescriptionResolver {

    /**
     * Returns a {@link ResourceBundle} for the given {@link Locale}, or {@code null} if this resolver
     * is not based on resource bundles.
     * <p>
     * This method will be invoked at least once before a series of invocations of the other methods in this
     * interface, and the returned bundle will be passed to those methods as a parameter. The intent is to cache
     * a resource bundle on the stack during the execution of all the methods needed to describe a resource or operation.
     * </p>
     *
     * @param locale the locale
     * @return the resource bundle, or {@code null}
     */
    ResourceBundle getResourceBundle(Locale locale);

    /**
     * Gets the description of the resource.
     *
     * @param locale the locale
     * @param bundle a resource bundle previously obtained from a call to {@link #getResourceBundle(java.util.Locale)},
     *               or {@code null} if that call returned {@code null}
     * @return the localized description
     */
    String getResourceDescription(Locale locale, ResourceBundle bundle);

    /**
     * Gets the description of one of the resource's attributes.
     *
     * @param attributeName the name of the attribute
     * @param locale the locale
     * @param bundle a resource bundle previously obtained from a call to {@link #getResourceBundle(java.util.Locale)},
     *               or {@code null} if that call returned {@code null}
     * @return the localized description
     */
    String getResourceAttributeDescription(String attributeName, Locale locale, ResourceBundle bundle);

    /**
     * Gets the description of a portion of a complex value type of one of the resource's attributes.
     *
     * @param attributeName the name of the attribute
     * @param locale the locale
     * @param bundle a resource bundle previously obtained from a call to {@link #getResourceBundle(java.util.Locale)},
     *               or {@code null} if that call returned {@code null}
     * @param suffixes suffixes to dot-append to the base attribute key to generate a key
     * @return the localized description
     */
    String getResourceAttributeValueTypeDescription(String attributeName, Locale locale, ResourceBundle bundle, String... suffixes);

    /**
     * Gets the description of one of the resource's operations.
     *
     * @param operationName the name of the operation
     * @param locale the locale
     * @param bundle a resource bundle previously obtained from a call to {@link #getResourceBundle(java.util.Locale)},
     *               or {@code null} if that call returned {@code null}
     * @return the localized description
     */
    String getOperationDescription(String operationName, Locale locale, ResourceBundle bundle);

    /**
     * Gets the description of one of the resource's operation's parameters.
     *
     * @param operationName the name of the operation
     * @param paramName the name of the operation's parameter
     * @param locale the locale
     * @param bundle a resource bundle previously obtained from a call to {@link #getResourceBundle(java.util.Locale)},
     *               or {@code null} if that call returned {@code null}
     * @return the localized description
     */
    String getOperationParameterDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle);

    /**
     * Gets the description of a portion of a complex value type of one of the resource's operation's parameters.
     *
     * @param operationName the name of the operation
     * @param paramName the name of the operation's parameter
     * @param locale the locale
     * @param bundle a resource bundle previously obtained from a call to {@link #getResourceBundle(java.util.Locale)},
     *               or {@code null} if that call returned {@code null}
     * @param suffixes suffixes to dot-append to the base attribute key to generate a key
     * @return the localized description
     */
    String getOperationParameterValueTypeDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle, String... suffixes);

    /**
     * Gets the description of the reply value for one of the resource's operations, or {@code null} if there is
     * no description.
     *
     * @param operationName the name of the operation
     * @param locale the locale
     * @param bundle a resource bundle previously obtained from a call to {@link #getResourceBundle(java.util.Locale)},
     *               or {@code null} if that call returned {@code null}
     * @return the localized description , or {@code null}
     */
    String getOperationReplyDescription(String operationName, Locale locale, ResourceBundle bundle);

    /**
     * Gets the description of a portion of a complex value type  of the reply value for one of the resource's operations, or {@code null} if there is
     * no description.
     *
     * @param operationName the name of the operation
     * @param locale the locale
     * @param bundle a resource bundle previously obtained from a call to {@link #getResourceBundle(java.util.Locale)},
     *               or {@code null} if that call returned {@code null}
     * @param suffixes suffixes to dot-append to the base attribute key to generate a key
     * @return the localized description , or {@code null}
     */
    String getOperationReplyValueTypeDescription(String operationName, Locale locale, ResourceBundle bundle, String... suffixes);


    /**
     * Gets the description of one of the resource's child types.
     *
     * @param childType the name of the child type
     * @param locale the locale
     * @param bundle a resource bundle previously obtained from a call to {@link #getResourceBundle(java.util.Locale)},
     *               or {@code null} if that call returned {@code null}
     * @return the localized description
         */
    String getChildTypeDescription(String childType, Locale locale, ResourceBundle bundle);


    /**
     * Gets the description of the resource.
     *
     * @param locale the locale
     * @param bundle a resource bundle previously obtained from a call to {@link #getResourceBundle(java.util.Locale)},
     *               or {@code null} if that call returned {@code null}
     * @return the localized description
     */
    String getResourceDeprecatedDescription(Locale locale, ResourceBundle bundle);

    /**
     * Gets the description of one of the resource's attributes.
     *
     * @param attributeName the name of the attribute
     * @param locale        the locale
     * @param bundle        a resource bundle previously obtained from a call to {@link #getResourceBundle(java.util.Locale)},
     *                      or {@code null} if that call returned {@code null}
     * @return the localized description
     */
    String getResourceAttributeDeprecatedDescription(String attributeName, Locale locale, ResourceBundle bundle);

    /**
     * Gets the description of one of the resource's operations.
     *
     * @param operationName the name of the operation
     * @param locale        the locale
     * @param bundle        a resource bundle previously obtained from a call to {@link #getResourceBundle(java.util.Locale)},
     *                      or {@code null} if that call returned {@code null}
     * @return the localized description
     */
    String getOperationDeprecatedDescription(String operationName, Locale locale, ResourceBundle bundle);

    /**
     * Gets the description of one of the resource's operation's parameters.
     *
     * @param operationName the name of the operation
     * @param paramName     the name of the operation's parameter
     * @param locale        the locale
     * @param bundle        a resource bundle previously obtained from a call to {@link #getResourceBundle(java.util.Locale)},
     *                      or {@code null} if that call returned {@code null}
     * @return the localized description
     */
    String getOperationParameterDeprecatedDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle);
}
