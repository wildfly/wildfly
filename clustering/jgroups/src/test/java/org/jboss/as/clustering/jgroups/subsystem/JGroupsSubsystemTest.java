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
package org.jboss.as.clustering.jgroups.subsystem;

import java.io.IOException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationConfiguration;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JGroupsSubsystemTest extends AbstractSubsystemBaseTest {

    public JGroupsSubsystemTest() {
        // FIXME JGroupsSubsystemTest constructor
        super(JGroupsExtension.SUBSYSTEM_NAME, new JGroupsExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        //TODO: This is copied from standalone-ha.xml you may want to try more combinations
        return "<subsystem xmlns=\"urn:jboss:domain:jgroups:1.0\" default-stack=\"udp\">" +
            "  <stack name=\"udp\">" +
            "    <transport type=\"UDP\" socket-binding=\"jgroups-udp\" diagnostics-socket-binding=\"jgroups-diagnostics\"/>" +
            "    <protocol type=\"PING\"/>" +
            "    <protocol type=\"MERGE2\"/>" +
            "    <protocol type=\"FD_SOCK\" socket-binding=\"jgroups-udp-fd\"/>" +
            "    <protocol type=\"FD\"/>" +
            "    <protocol type=\"VERIFY_SUSPECT\"/>" +
            "    <protocol type=\"BARRIER\"/>" +
            "    <protocol type=\"pbcast.NAKACK\"/>" +
            "    <protocol type=\"UNICAST\"/>" +
            "    <protocol type=\"pbcast.STABLE\"/>" +
            "    <protocol type=\"VIEW_SYNC\"/>" +
            "    <protocol type=\"pbcast.GMS\"/>" +
            "    <protocol type=\"UFC\"/>" +
            "    <protocol type=\"MFC\"/>" +
            "    <protocol type=\"FRAG2\"/>" +
            "    <protocol type=\"pbcast.STREAMING_STATE_TRANSFER\"/>" +
            "    <protocol type=\"pbcast.FLUSH\"/>" +
            "  </stack>" +
            "</subsystem>";
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization(){
            @Override
            protected OperationContext.Type getType() {
                return OperationContext.Type.MANAGEMENT;
            }


            @Override
            protected ValidationConfiguration getModelValidationConfiguration() {
                //TODO fix validation https://issues.jboss.org/browse/AS7-1787
                return null;
            }
        };
    }

}
