/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.BeanManager;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Filter;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.metadata.BeansXmlImpl;
import org.jboss.weld.metadata.ScanningImpl;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import static org.jboss.weld.bootstrap.spi.BeansXml.EMPTY_BEANS_XML;
import static org.jboss.weld.logging.messages.XmlMessage.LOAD_ERROR;

/**
 * Fork of {@link org.jboss.weld.xml.BeansXmlParser} to fix some minor XML parsing issues.
 *
 * @author Stuart Douglas
 * @author Pete Muir
 */
public class BeansXmlParser {


    private static final InputSource[] EMPTY_INPUT_SOURCE_ARRAY = new InputSource[0];

    public BeansXml parse(final URL beansXml, final PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(true);
        if (beansXml == null) {
            throw new org.jboss.weld.exceptions.IllegalStateException(LOAD_ERROR, "unknown");
        }
        SAXParser parser;
        try {
            parser = factory.newSAXParser();
        } catch (SAXException e) {
            throw new DeploymentUnitProcessingException(e);
        } catch (ParserConfigurationException e) {
            throw new DeploymentUnitProcessingException(e);
        }
        InputStream beansXmlInputStream = null;
        try {
            beansXmlInputStream = beansXml.openStream();
            InputSource source = new InputSource(beansXmlInputStream);
            if (source.getByteStream().available() == 0) {
                // The file is just acting as a marker file
                return EMPTY_BEANS_XML;
            }
            BeansXmlHandler handler = new BeansXmlHandler(beansXml, propertyReplacer);

            try {
                parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
                parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", loadXsds());
            } catch (SAXNotRecognizedException e) {
                // No op, we just don't validate the XML
            } catch (SAXNotSupportedException e) {
                // No op, we just don't validate the XML
            }

            parser.parse(source, handler);

            return handler.createBeansXml();
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException("IOException parsing " + beansXml,e);
        } catch (SAXException e) {
            throw new DeploymentUnitProcessingException("SAXException parsing " + beansXml,e);
        } finally {
            if (beansXmlInputStream != null) {
                try {
                    beansXmlInputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public BeansXml parse(Iterable<URL> urls, final PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException {
        List<Metadata<String>> alternativeStereotypes = new ArrayList<Metadata<String>>();
        List<Metadata<String>> alternativeClasses = new ArrayList<Metadata<String>>();
        List<Metadata<String>> decorators = new ArrayList<Metadata<String>>();
        List<Metadata<String>> interceptors = new ArrayList<Metadata<String>>();
        List<Metadata<Filter>> includes = new ArrayList<Metadata<Filter>>();
        List<Metadata<Filter>> excludes = new ArrayList<Metadata<Filter>>();
        for (URL url : urls) {
            BeansXml beansXml = parse(url, propertyReplacer);
            alternativeStereotypes.addAll(beansXml.getEnabledAlternativeStereotypes());
            alternativeClasses.addAll(beansXml.getEnabledAlternativeClasses());
            decorators.addAll(beansXml.getEnabledDecorators());
            interceptors.addAll(beansXml.getEnabledInterceptors());
            includes.addAll(beansXml.getScanning().getIncludes());
            excludes.addAll(beansXml.getScanning().getExcludes());
        }
        return new BeansXmlImpl(alternativeClasses, alternativeStereotypes, decorators, interceptors, new ScanningImpl(includes, excludes));
    }

    private static InputSource[] loadXsds() {
        List<InputSource> xsds = new ArrayList<InputSource>();
        // The Weld xsd
        InputSource weldXsd = loadXsd("beans_1_1.xsd", BeansXmlParser.class.getClassLoader());
        // The CDI Xsd
        InputSource cdiXsd = loadXsd("beans_1_0.xsd", BeanManager.class.getClassLoader());
        if (weldXsd != null) {
            xsds.add(weldXsd);
        }
        if (cdiXsd != null) {
            xsds.add(cdiXsd);
        }
        return xsds.toArray(EMPTY_INPUT_SOURCE_ARRAY);
    }


    private static InputSource loadXsd(String name, ClassLoader classLoader) {
        InputStream in = classLoader.getResourceAsStream(name);
        if (in == null) {
            return null;
        } else {
            return new InputSource(in);
        }
    }


}
