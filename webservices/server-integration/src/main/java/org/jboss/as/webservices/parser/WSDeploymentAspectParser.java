/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.parser;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.jboss.wsf.spi.util.StAXUtils.match;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.ws.WebServiceException;

import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.ws.common.JavaUtils;
import org.jboss.wsf.spi.deployment.DeploymentAspect;
import org.jboss.wsf.spi.util.StAXUtils;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A parser for WS deployment aspects
 *
 * @author alessio.soldano@jboss.com
 * @since 18-Jan-2011
 *
 */
public class WSDeploymentAspectParser {

    private static final String NS = "urn:jboss:ws:deployment:aspects:1.0";
    private static final String DEPLOYMENT_ASPECTS = "deploymentAspects";
    private static final String DEPLOYMENT_ASPECT = "deploymentAspect";
    private static final String CLASS = "class";
    private static final String PRIORITY = "priority";
    private static final String PROPERTY = "property";
    private static final String NAME = "name";
    private static final String MAP = "map";
    private static final String KEY_CLASS = "keyClass";
    private static final String VALUE_CLASS = "valueClass";
    private static final String ENTRY = "entry";
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String LIST = "list";
    private static final String ELEMENT_CLASS = "elementClass";

    public static List<DeploymentAspect> parse(InputStream is, ClassLoader loader) {
        try {
            XMLStreamReader xmlr = StAXUtils.createXMLStreamReader(is);
            return parse(xmlr, loader);
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    public static List<DeploymentAspect> parse(XMLStreamReader reader, ClassLoader loader) throws XMLStreamException {
        int iterate;
        try {
            iterate = reader.nextTag();
        } catch (XMLStreamException e) {
            // skip non-tag elements
            iterate = reader.nextTag();
        }
        List<DeploymentAspect> deploymentAspects = null;
        switch (iterate) {
            case END_ELEMENT: {
                // we're done
                break;
            }
            case START_ELEMENT: {

                if (match(reader, NS, DEPLOYMENT_ASPECTS)) {
                    deploymentAspects = parseDeploymentAspects(reader, loader);
                } else {
                    throw WSLogger.ROOT_LOGGER.unexpectedElement(reader.getLocalName());
                }
            }
        }
        return deploymentAspects;
    }

    private static List<DeploymentAspect> parseDeploymentAspects(XMLStreamReader reader, ClassLoader loader) throws XMLStreamException {
        List<DeploymentAspect> deploymentAspects = new LinkedList<DeploymentAspect>();
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    if (match(reader, NS, DEPLOYMENT_ASPECTS)) {
                        return deploymentAspects;
                    } else {
                        throw WSLogger.ROOT_LOGGER.unexpectedEndTag(reader.getLocalName());
                    }
                }
                case XMLStreamConstants.START_ELEMENT: {
                    if (match(reader, NS, DEPLOYMENT_ASPECT)) {
                        deploymentAspects.add(parseDeploymentAspect(reader, loader));
                    } else {
                        throw WSLogger.ROOT_LOGGER.unexpectedElement(reader.getLocalName());
                    }
                }
            }
        }
        throw WSLogger.ROOT_LOGGER.unexpectedEndOfDocument();
    }

    private static DeploymentAspect parseDeploymentAspect(XMLStreamReader reader, ClassLoader loader) throws XMLStreamException {
        String deploymentAspectClass = reader.getAttributeValue(null, CLASS);
        if (deploymentAspectClass == null) {
            throw WSLogger.ROOT_LOGGER.missingDeploymentAspectClassAttribute();
        }
        DeploymentAspect deploymentAspect = null;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends DeploymentAspect> clazz = (Class<? extends DeploymentAspect>) Class.forName(deploymentAspectClass, true, loader);
            ClassLoader orig = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(loader);
                deploymentAspect = clazz.newInstance();
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(orig);
            }
        } catch (Exception e) {
            throw WSLogger.ROOT_LOGGER.cannotInstantiateDeploymentAspect(e, deploymentAspectClass);
        }
        String priority = reader.getAttributeValue(null, PRIORITY);
        if (priority != null) {
            deploymentAspect.setRelativeOrder(Integer.parseInt(priority.trim()));
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    if (match(reader, NS, DEPLOYMENT_ASPECT)) {
                        return deploymentAspect;
                    } else {
                        throw WSLogger.ROOT_LOGGER.unexpectedEndTag(reader.getLocalName());
                    }
                }
                case XMLStreamConstants.START_ELEMENT: {
                    if (match(reader, NS, PROPERTY)) {
                        parseProperty(reader, deploymentAspect, loader);
                    } else {
                        throw WSLogger.ROOT_LOGGER.unexpectedElement(reader.getLocalName());
                    }
                }
            }
        }
        throw WSLogger.ROOT_LOGGER.unexpectedEndOfDocument();
    }

    @SuppressWarnings("rawtypes")
    private static void parseProperty(XMLStreamReader reader, DeploymentAspect deploymentAspect, ClassLoader loader) throws XMLStreamException {
        Class<? extends DeploymentAspect> deploymentAspectClass = deploymentAspect.getClass();
        String propName = reader.getAttributeValue(null, NAME);
        if (propName == null) {
            throw WSLogger.ROOT_LOGGER.missingPropertyNameAttribute(deploymentAspect);
        }
        String propClass = reader.getAttributeValue(null, CLASS);
        if (propClass == null) {
            throw WSLogger.ROOT_LOGGER.missingPropertyClassAttribute(deploymentAspect);
        } else {
            try {
                if (isSupportedPropertyClass(propClass)) {
                    Method m = selectMethod(deploymentAspectClass, propName, propClass);
                    m.invoke(deploymentAspect, parseSimpleValue(reader, propClass));
                    return;
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    if (match(reader, NS, PROPERTY)) {
                        return;
                    } else {
                        throw WSLogger.ROOT_LOGGER.unexpectedEndTag(reader.getLocalName());
                    }
                }
                case XMLStreamConstants.START_ELEMENT: {
                    if (match(reader, NS, MAP)) {
                        try {
                            Method m = selectMethod(deploymentAspectClass, propName, propClass);
                            Map map = parseMapProperty(reader, propClass, reader.getAttributeValue(null, KEY_CLASS),
                                    reader.getAttributeValue(null, VALUE_CLASS), loader);
                            m.invoke(deploymentAspect, map);
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    } else if (match(reader, NS, LIST)) {
                        try {
                            Method m = selectMethod(deploymentAspectClass, propName, propClass);
                            List list = parseListProperty(reader, propClass, reader.getAttributeValue(null, ELEMENT_CLASS));
                            m.invoke(deploymentAspect, list);
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    } else {
                        throw WSLogger.ROOT_LOGGER.unexpectedElement(reader.getLocalName());
                    }
                }
            }
        }
        throw WSLogger.ROOT_LOGGER.unexpectedEndOfDocument();
    }

    private static Method selectMethod(Class<?> deploymentAspectClass, String propName, String propClass) throws ClassNotFoundException {
        //TODO improve this (better support for primitives, edge cases, etc.)
        Method[] methods = deploymentAspectClass.getMethods();
        for (Method m : methods) {
            if (m.getName().equals("set" + JavaUtils.capitalize(propName))) {
                Class<?>[] pars = m.getParameterTypes();
                if (pars.length == 1 && (propClass.equals(pars[0].getName()) || (pars[0].isAssignableFrom(Class.forName(propClass))))) {
                    return m;
                }
            }
        }
        return null;
    }

    private static boolean isSupportedPropertyClass(String propClass) {
        return (String.class.getName().equals(propClass) || Boolean.class.getName().equals(propClass) || Integer.class
                .getName().equals(propClass) || JavaUtils.isPrimitive(propClass));
    }

    private static Object parseSimpleValue(XMLStreamReader reader, String propClass) throws XMLStreamException {
        if (String.class.getName().equals(propClass)) {
            return StAXUtils.elementAsString(reader);
        } else if (Boolean.class.getName().equals(propClass)) {
            return StAXUtils.elementAsBoolean(reader);
        } else if (Integer.class.getName().equals(propClass)) {
            return StAXUtils.elementAsInt(reader);
        } else if (boolean.class.getName().equals(propClass)) {
            return StAXUtils.elementAsBoolean(reader);
        } else {
            throw WSLogger.ROOT_LOGGER.unsupportedPropertyClass(propClass);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List parseListProperty(XMLStreamReader reader, String propClass, String elementClass)
            throws XMLStreamException {
        List list = null;
        try {
            list = (List) Class.forName(propClass).newInstance();
        } catch (Exception e) {
            throw WSLogger.ROOT_LOGGER.cannotInstantiateList(e, propClass);
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    if (match(reader, NS, LIST)) {
                        return list;
                    } else {
                        throw WSLogger.ROOT_LOGGER.unexpectedEndTag(reader.getLocalName());
                    }
                }
                case XMLStreamConstants.START_ELEMENT: {
                    if (match(reader, NS, VALUE)) {
                        list.add(parseSimpleValue(reader, elementClass));
                    } else {
                        throw WSLogger.ROOT_LOGGER.unexpectedElement(reader.getLocalName());
                    }
                }
            }
        }
        throw WSLogger.ROOT_LOGGER.unexpectedEndOfDocument();
    }

    @SuppressWarnings("rawtypes")
    private static Map parseMapProperty(XMLStreamReader reader, String propClass, String keyClass, String valueClass, ClassLoader loader)
            throws XMLStreamException {
        Map map = null;
        try {
            map = (Map) Class.forName(propClass, true, loader).newInstance();
        } catch (Exception e) {
            throw WSLogger.ROOT_LOGGER.cannotInstantiateMap(e, propClass);
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    if (match(reader, NS, MAP)) {
                        return map;
                    } else {
                        throw WSLogger.ROOT_LOGGER.unexpectedEndTag(reader.getLocalName());
                    }
                }
                case XMLStreamConstants.START_ELEMENT: {
                    if (match(reader, NS, ENTRY)) {
                        parseMapEntry(reader, map, keyClass, valueClass);
                    } else {
                        throw WSLogger.ROOT_LOGGER.unexpectedElement(reader.getLocalName());
                    }
                }
            }
        }
        throw WSLogger.ROOT_LOGGER.unexpectedEndOfDocument();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void parseMapEntry(XMLStreamReader reader, Map map, String keyClass, String valueClass)
            throws XMLStreamException {
        boolean keyStartDone = false, valueStartDone = false;
        Object key = null;
        Object value = null;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    if (match(reader, NS, ENTRY) && keyStartDone && valueStartDone) {
                        map.put(key, value);
                        return;
                    } else {
                        throw WSLogger.ROOT_LOGGER.unexpectedEndTag(reader.getLocalName());
                    }
                }
                case XMLStreamConstants.START_ELEMENT: {
                    if (match(reader, NS, KEY)) {
                        keyStartDone = true;
                        key = parseSimpleValue(reader, keyClass);
                    } else if (match(reader, NS, VALUE)) {
                        valueStartDone = true;
                        value = parseSimpleValue(reader, valueClass);
                    } else {
                        throw WSLogger.ROOT_LOGGER.unexpectedElement(reader.getLocalName());
                    }
                }
            }
        }
        throw WSLogger.ROOT_LOGGER.unexpectedEndOfDocument();
    }
}
