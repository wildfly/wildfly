/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.model.base;

import junit.framework.TestCase;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLMapper;

/**
 * Base class for unit-tests of {@link AbstractModelElement} subclasses.
 * 
 * @author Brian Stansberry
 */
public abstract class DomainModelElementTestBase extends TestCase {

    private XMLMapper mapper;
    
    /**
     * @param name
     */
    public DomainModelElementTestBase(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mapper = createXMLMapper();        
    }
    
    /**
     * Tests that the element can be serialized, a new instance can be 
     * deserialized, and that the two objects will be equivalent.
     * @throws Exception
     */
    public abstract void testSerializationDeserialization() throws Exception;
    
    /**
     * Test that the object can be parsed from XML, written back to XML, a new
     * instance reparsed from that output, and that the two parsed objects will
     * be equivalent.
     * 
     * @throws Exception
     */
//    public abstract void testXMLRoundTrip() throws Exception;
    
    
    protected XMLMapper getXMLMapper() {
        return mapper;
    }
    
    /**
     * Create the XMLMapper that will be returned by {@link #getXMLMapper()}
     * 
     * @return
     * @throws Exception
     */
    protected abstract XMLMapper createXMLMapper() throws Exception;

    /**
     * Gets the namespace that will be used in tests.
     * 
     * @return
     */
    protected abstract String getTargetNamespace();

    /**
     * Gets the schema location of the namespace that will be used in tests.
     * 
     * @return
     */
    protected abstract String getTargetNamespaceLocation();

}
