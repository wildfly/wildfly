/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.lazyconnectionmanager;

import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyConnection;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyConnectionFactory;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyConnectionFactoryImpl;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyConnectionImpl;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyLocalTransaction;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyManagedConnection;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyManagedConnectionFactory;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyManagedConnectionMetaData;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyResourceAdapter;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyXAResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;

/**
 * @author <a href="mailto:jesper.pedersen@ironjacamar.org">Jesper Pedersen</a>
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
public abstract class LazyAssociationAbstractTestCase {
    protected static final String RAR_NAME = "lazy.rar";
    protected static final String LIB_JAR_NAME = "common.jar";

    protected static Archive<ResourceAdapterArchive> createResourceAdapter(String raFileName,
                                                                           String configurationFileName,
                                                                           Class testClass) {
        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, RAR_NAME);
        rar.addAsManifestResource(LazyResourceAdapter.class.getPackage(), raFileName, "ra.xml");
        rar.addAsManifestResource(LazyResourceAdapter.class.getPackage(), configurationFileName, "ironjacamar.xml");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, LIB_JAR_NAME);
        jar.addClass(LazyResourceAdapter.class)
                .addClass(LazyManagedConnectionFactory.class)
                .addClass(LazyManagedConnection.class)
                .addClass(LazyConnection.class)
                .addClass(LazyConnectionImpl.class)
                .addClass(LazyXAResource.class)
                .addClass(LazyLocalTransaction.class)
                .addClass(LazyManagedConnectionMetaData.class)
                .addClass(LazyConnectionFactory.class)
                .addClass(LazyConnectionFactoryImpl.class);

        jar.addClass(LazyAssociationAbstractTestCase.class);
        jar.addClass(testClass);

        rar.addAsLibrary(jar);
        return rar;
    }
}
