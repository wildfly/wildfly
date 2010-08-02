/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment.item;

import org.jboss.as.model.DeploymentUnitKey;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassTable;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Temporary registry to hold onto deployment items used for deployments.
 * 
 * @author John E. Bailey
 */
public class DeploymentItemRegistry {

    private static final Map<DeploymentUnitKey, List<DeploymentItem>> cache = new HashMap<DeploymentUnitKey, List<DeploymentItem>>();
    private static MarshallerFactory marshallerFactory;
    private static final MarshallingConfiguration marshallingConfiguration;

    private static ClassTable classTable;
    static {
        marshallingConfiguration= new MarshallingConfiguration();
        try {
            marshallerFactory = Marshalling.getMarshallerFactory("river", ModuleClassLoader.forModuleName("org.jboss.marshalling:jboss-marshalling-river"));
            marshallingConfiguration.setClassTable(ModularClassTable.getInstance());
        } catch (ModuleLoadException e) {
            marshallerFactory = Marshalling.getMarshallerFactory("river");
        }
    }

    public static void registerDeploymentItems(final DeploymentUnitKey key, List<DeploymentItem> deploymentItems) {
        cache.put(key, deploymentItems);
        try {
            final Marshaller marshaller = marshallerFactory.createMarshaller(marshallingConfiguration);
            // TODO:  Create file output
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            marshaller.start(Marshalling.createByteOutput(byteArrayOutputStream));

            marshaller.writeInt(deploymentItems.size());
            for(DeploymentItem deploymentItem : deploymentItems) {
                marshaller.writeObject(deploymentItem);
            }
            marshaller.finish();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<DeploymentItem> getDeploymentItems(final DeploymentUnitKey key) {
        List<DeploymentItem> items = cache.get(key);
        if(items != null)
            return items;
        return null; // TODO: Read serialized items
    }
}
