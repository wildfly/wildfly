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
package org.jboss.as.connector.subsystems.jca;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JcaSubsystemTestCase extends AbstractSubsystemBaseTest {

    public JcaSubsystemTestCase() {
        // FIXME JcaSubsystemTestCase constructor
        super(JcaExtension.SUBSYSTEM_NAME, new JcaExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        //TODO: This is copied from standalone.xml you may want to try more combinations
        return
            "<subsystem xmlns=\"urn:jboss:domain:jca:1.0\">" +
            "    <archive-validation enabled=\"false\" />" +
            "    <bean-validation enabled=\"false\" />" +
            "    <default-workmanager>" +
            "        <short-running-threads blocking=\"true\">" +
            "                <core-threads count=\"10\" per-cpu=\"20\"/>" +
            "                <queue-length count=\"10\" per-cpu=\"20\"/>" +
            "                <max-threads count=\"10\" per-cpu=\"20\"/>" +
            "                <keepalive-time time=\"10\" unit=\"seconds\"/>" +
            "        </short-running-threads>" +
            "        <long-running-threads blocking=\"true\">" +
            "                <core-threads count=\"10\" per-cpu=\"20\"/>" +
            "                <queue-length count=\"10\" per-cpu=\"20\"/>" +
            "                <max-threads count=\"10\" per-cpu=\"20\"/>" +
            "                <keepalive-time time=\"10\" unit=\"seconds\"/>" +
            "        </long-running-threads>" +
            "    </default-workmanager>" +
            "</subsystem>";
    }

    //TODO AS7-2421 remove me
    protected boolean testRemoval() {
        return false;
    }

}
