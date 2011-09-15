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
package org.jboss.as.logging;

import java.io.IOException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationConfiguration;
import org.junit.Ignore;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Ignore("The marshalling does not seem to be right - AS7-1792 ")
public class LoggingSubsystemTestCase extends AbstractSubsystemBaseTest {

    public LoggingSubsystemTestCase() {
        super(LoggingExtension.SUBSYSTEM_NAME, new LoggingExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        //TODO THis is copied from standalone.xml. Testing more combinations is a good idea
        return
            "<subsystem xmlns=\"urn:jboss:domain:logging:1.1\">" +
            "    <console-handler name=\"CONSOLE\">" +
            "        <level name=\"INFO\"/>" +
            "        <formatter>" +
            "            <pattern-formatter pattern=\"%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n\"/>" +
            "        </formatter>" +
            "    </console-handler>" +
            "    <periodic-rotating-file-handler name=\"FILE\">" +
            "        <level name=\"INFO\"/>" +
            "        <formatter>" +
            "            <pattern-formatter pattern=\"%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n\"/>" +
            "        </formatter>" +
            "        <file relative-to=\"jboss.server.log.dir\" path=\"server.log\"/>" +
            "        <suffix value=\".yyyy-MM-dd\"/>" +
            "    </periodic-rotating-file-handler>" +
            "    <logger category=\"com.arjuna\">" +
            "        <level name=\"WARN\"/>" +
            "    </logger>" +
            "    <logger category=\"org.apache.tomcat.util.modeler\">" +
            "        <level name=\"WARN\"/>" +
            "    </logger>" +
            "    <logger category=\"sun.rmi\">" +
            "        <level name=\"WARN\"/>" +
            "    </logger>" +
            "    <root-logger>" +
            "        <level name=\"INFO\"/>" +
            "        <handlers>" +
            "            <handler name=\"CONSOLE\"/>" +
            "            <handler name=\"FILE\"/>" +
            "        </handlers>" +
            "    </root-logger>" +
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
                //TODO fix providers https://issues.jboss.org/browse/AS7-1792
                return null;
            }

            @Override
            protected boolean isValidateOperations() {
                //TODO fix providers https://issues.jboss.org/browse/AS7-1792
                return false;
            }
        };
    }
}
