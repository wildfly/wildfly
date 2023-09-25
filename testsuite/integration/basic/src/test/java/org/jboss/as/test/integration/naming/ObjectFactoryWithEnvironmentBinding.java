/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

/**
 * An {@link ObjectFactory} which verifies the environment received on
 * {@link ObjectFactory#getObjectInstance(Object, Name, Context, Hashtable)}, is the same that a test case used in the factory's
 * binding operation.
 *
 * @author Eduardo Martins
 * @author Stuart Douglas
 *
 */
public class ObjectFactoryWithEnvironmentBinding implements ObjectFactory {

    @Override
    public Object getObjectInstance(final Object obj, final Name name, final Context nameCtx, final Hashtable<?, ?> environment) throws Exception {
        ObjectFactoryWithEnvironmentBindingTestCase.validateEnvironmentProperties(environment);
        return environment.get("p1");
    }
}
