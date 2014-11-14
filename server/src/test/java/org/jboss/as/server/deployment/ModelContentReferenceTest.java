/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.server.deployment;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.repository.ContentReference;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class ModelContentReferenceTest {

    public ModelContentReferenceTest() {
    }

    /**
     * Test of fromDeploymentName method, of class ModelContentReference.
     */
    @Test
    public void testFromDeploymentName_String_String() {
        String name = "wildfly-ejb-in-war.war";
        String hash = "48d7b49e084860769d5ce03dc2223466aa46be3a";
        PathAddress address = PathAddress.pathAddress(PathElement.pathElement("deployment", "wildfly-ejb-in-war.war"));
        ContentReference result = ModelContentReference.fromDeploymentName(name, hash);
        ContentReference expResult = new ContentReference(address.toCLIStyleString(), "48d7b49e084860769d5ce03dc2223466aa46be3a");
        assertThat(result, is(expResult));
    }

    /**
     * Test of fromDeploymentName method, of class ModelContentReference.
     */
    @Test
    public void testFromDeploymentName_String_byteArr() {
        String name = "wildfly-ejb-in-war.war";
        byte[] hash = HashUtil.hexStringToByteArray("48d7b49e084860769d5ce03dc2223466aa46be3a");
        PathAddress address = PathAddress.pathAddress(PathElement.pathElement("deployment", "wildfly-ejb-in-war.war"));
        ContentReference result = ModelContentReference.fromDeploymentName(name, hash);
        ContentReference expResult = new ContentReference(address.toCLIStyleString(), "48d7b49e084860769d5ce03dc2223466aa46be3a");
        assertThat(result, is(expResult));
    }

     /**
     * Test of fromDeploymentName method, of class ModelContentReference.
     */
    @Test
    public void testFromDeploymentAddress() {
        byte[] hash = HashUtil.hexStringToByteArray("48d7b49e084860769d5ce03dc2223466aa46be3a");
        PathAddress address = PathAddress.pathAddress(PathElement.pathElement("deployment", "wildfly-ejb-in-war.war"));
        ContentReference result = ModelContentReference.fromModelAddress(address, hash);
        ContentReference expResult = new ContentReference(address.toCLIStyleString(), "48d7b49e084860769d5ce03dc2223466aa46be3a");
        assertThat(result, is(expResult));
    }

}
