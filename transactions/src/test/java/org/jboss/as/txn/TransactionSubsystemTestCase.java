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
package org.jboss.as.txn;

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
@Ignore
public class TransactionSubsystemTestCase extends AbstractSubsystemBaseTest {

    public TransactionSubsystemTestCase() {
        super(TransactionExtension.SUBSYSTEM_NAME, new TransactionExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        //This is just copied from standalone.xml testing more combinations would be good
        return
            "<subsystem xmlns=\"urn:jboss:domain:transactions:1.0\" >" +
            "    <recovery-environment socket-binding=\"txn-recovery-environment\" status-socket-binding=\"txn-status-manager\"/>" +
            "    <core-environment>" +
            "        <process-id>" +
            "            <uuid />" +
            "        </process-id>" +
            "    </core-environment>" +
            "    <coordinator-environment default-timeout=\"300\"/>" +
            "</subsystem>";
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization(){
            @Override
            protected OperationContext.Type getType() {
                return OperationContext.Type.MANAGEMENT;
            }

        };
    }
}
