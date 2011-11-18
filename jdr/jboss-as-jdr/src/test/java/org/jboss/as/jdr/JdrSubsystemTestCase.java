
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
package org.jboss.as.jdr;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.dmr.ModelNode;
import org.junit.Test;


/**
 * Performs basic parsing and configuration testing of the JDR subsystem.
 * 
 * @author Mike M. Clark
 */
public class JdrSubsystemTestCase extends AbstractSubsystemBaseTest {
	
	public JdrSubsystemTestCase() {
		super(JdrReportExtension.SUBSYSTEM_NAME, new JdrReportExtension());
	}
	
	@Test
	public void testParseEmptySubsystem() throws Exception {
		String subsystemXml = "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\"></subsystem>";
		List<ModelNode> operations = super.parse(subsystemXml);
		
		// Should have just one operation (add)
		Assert.assertEquals("Wrong number of operations encountered:", 1, operations.size());
		
		// Validate add operation content
		ModelNode addOperation = operations.get(0);
		Assert.assertEquals("Wrong operation encountered:", ModelDescriptionConstants.ADD,
				addOperation.get(ModelDescriptionConstants.OP).asString());
		
		PathAddress address = PathAddress.pathAddress(addOperation.get(ModelDescriptionConstants.OP_ADDR));
		Assert.assertEquals("Wrong path address size", 1, address.size());
		
		PathElement element = address.getElement(0);
		Assert.assertEquals("Wrong address key", ModelDescriptionConstants.SUBSYSTEM, element.getKey());
		Assert.assertEquals("Wrong address value", JdrReportExtension.SUBSYSTEM_NAME, element.getValue());
	}
	
	@Test(expected=XMLStreamException.class)
	public void testParseSubsystemWithBadChild() throws Exception {
		String subsystemXml =
            "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\"><invalid/></subsystem>";
		super.parse(subsystemXml);
	}
	
	@Test(expected=XMLStreamException.class)
	public void testParseSubsystemWithBadAttribute() throws Exception {
		String subsystemXml =
            "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\" attr=\"wrong\"/>";
		super.parse(subsystemXml);
	}

	@Override
	protected String getSubsystemXml() throws IOException {
		return readResource("subsystem.xml");
	}
}
