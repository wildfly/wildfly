/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.core.model.test.deployment;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import static org.hamcrest.CoreMatchers.containsString;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.model.test.ModelTestUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class StandaloneDeploymentUnicityTestCase extends AbstractCoreModelTest {

    @Test
    public void testDeployments() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder(TestModelType.STANDALONE)
            .setXmlResource("standalone.xml")
            .createContentRepositoryContent("12345678901234567890")
            .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());

        String marshalled = kernelServices.getPersistedSubsystemXml();
        ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), "standalone.xml"), marshalled);
    }

    @Test
    public void testIncorrectDeployments() throws Exception {try { createKernelServicesBuilder(TestModelType.STANDALONE)
            .setXmlResource("standalone_duplicate.xml")
            .createContentRepositoryContent("12345678901234567890")
            .build();
        } catch (XMLStreamException ex) {
            String expectedMessage = ControllerMessages.MESSAGES.duplicateNamedElement("abc.war", new Location() {
                public int getLineNumber() {
                    return 287;
                }

                public int getColumnNumber() {
                    return 1;
                }

                public int getCharacterOffset() {
                    return 1;
                }

                public String getPublicId() {
                    return "";
                }

                public String getSystemId() {
                    return "";
                }
            }).getMessage();
            expectedMessage = expectedMessage.substring(expectedMessage.indexOf("JBAS014664:"));
            Assert.assertThat(ex.getMessage(), containsString(expectedMessage));
        }
    }
}
