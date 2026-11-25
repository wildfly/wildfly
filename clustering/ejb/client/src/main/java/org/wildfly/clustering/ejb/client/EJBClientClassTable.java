/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.client;

import java.io.IOException;
import java.util.List;

import org.jboss.ejb.client.BasicSessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.jboss.ejb.client.UnknownSessionID;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Unmarshaller;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.jboss.IdentityClassTable;

@MetaInfServices(ClassTable.class)
public class EJBClientClassTable implements ClassTable {

    private final ClassTable table = new IdentityClassTable(List.of(BasicSessionID.class, UnknownSessionID.class, UUIDSessionID.class));

    @Override
    public Writer getClassWriter(Class<?> clazz) throws IOException {
        return this.table.getClassWriter(clazz);
    }

    @Override
    public Class<?> readClass(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        return this.table.readClass(unmarshaller);
    }
}
