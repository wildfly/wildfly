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
package org.jboss.as.clustering.infinispan.subsystem;

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
public class InfinispanSubsystemTestCase extends AbstractSubsystemBaseTest {

    public InfinispanSubsystemTestCase() {
        super(InfinispanExtension.SUBSYSTEM_NAME, new InfinispanExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        //TODO: This is copied from standalone-ha.xml you may want to try more combinations

        return
        "<subsystem xmlns=\"urn:jboss:domain:infinispan:1.0\" default-cache-container=\"cluster\">" +
        "<cache-container name=\"cluster\" default-cache=\"default\">" +
        "    <alias>ha-partition</alias>" +
        "    <replicated-cache name=\"default\" mode=\"SYNC\" batching=\"true\">" +
        "        <locking isolation=\"REPEATABLE_READ\"/>" +
        "    </replicated-cache>" +
        "</cache-container>" +
        "<cache-container name=\"web\" default-cache=\"repl\">" +
        "    <alias>standard-session-cache</alias>" +
        "    <replicated-cache name=\"repl\" mode=\"ASYNC\" batching=\"true\">" +
        "        <locking isolation=\"REPEATABLE_READ\"/>" +
        "        <file-store/>" +
        "    </replicated-cache>" +
        "    <distributed-cache name=\"dist\" mode=\"ASYNC\" batching=\"true\">" +
        "        <locking isolation=\"REPEATABLE_READ\"/>" +
        "        <file-store/>" +
        "    </distributed-cache>" +
        "</cache-container>" +
        "<cache-container name=\"sfsb\" default-cache=\"repl\">" +
        "    <alias>sfsb-cache</alias>" +
        "    <alias>jboss.cache:service=EJB3SFSBClusteredCache</alias>" +
        "    <replicated-cache name=\"repl\" mode=\"ASYNC\" batching=\"true\">" +
        "        <locking isolation=\"REPEATABLE_READ\"/>" +
        "        <eviction strategy=\"LRU\" max-entries=\"10000\"/>" +
        "        <file-store/>" +
        "    </replicated-cache>" +
        "</cache-container>" +
        "<cache-container name=\"hibernate\" default-cache=\"local-query\">" +
        "    <invalidation-cache name=\"entity\" mode=\"SYNC\">" +
        "        <eviction strategy=\"LRU\" max-entries=\"10000\"/>" +
        "        <expiration max-idle=\"100000\"/>" +
        "    </invalidation-cache>" +
        "    <local-cache name=\"local-query\">" +
        "        <eviction strategy=\"LRU\" max-entries=\"10000\"/>" +
        "        <expiration max-idle=\"100000\"/>" +
        "    </local-cache>" +
        "    <replicated-cache name=\"timestamps\" mode=\"ASYNC\">" +
        "        <eviction strategy=\"NONE\"/>" +
        "    </replicated-cache>" +
        "</cache-container>" +
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
                //TODO fix validation https://issues.jboss.org/browse/AS7-1788
                return null;
            }
        };
    }


}
