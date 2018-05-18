/*
* JBoss, Home of Professional Open Source.
* Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.extension.clustering.singleton;

import java.util.EnumSet;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.clustering.subsystem.ClusteringSubsystemTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.singleton.SingletonCacheRequirement;
import org.wildfly.clustering.singleton.SingletonDefaultCacheRequirement;

/**
 * @author Paul Ferraro
 */
@RunWith(Parameterized.class)
public class SingletonSubsystemTestCase extends ClusteringSubsystemTest<SingletonSchema> {

    @Parameters
    public static Iterable<SingletonSchema> parameters() {
        return EnumSet.allOf(SingletonSchema.class);
    }

    public SingletonSubsystemTestCase(SingletonSchema schema) {
        super(SingletonExtension.SUBSYSTEM_NAME, new SingletonExtension(), schema, SingletonSchema.CURRENT, "subsystem-singleton-%d_%d.xml", "schema/wildfly-singleton_%d_%d.xsd");
    }

    @Override
    protected org.jboss.as.subsystem.test.AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization()
                .require(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING, "binding0", "binding1")
                .require(SingletonDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY, "singleton-container")
                .require(SingletonCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY, "singleton-container", "singleton-cache")
                ;
    }
}
