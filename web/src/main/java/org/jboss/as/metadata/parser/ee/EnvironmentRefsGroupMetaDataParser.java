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

package org.jboss.as.metadata.parser.ee;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.metadata.javaee.spec.DataSourcesMetaData;
import org.jboss.metadata.javaee.spec.EJBLocalReferencesMetaData;
import org.jboss.metadata.javaee.spec.EJBReferencesMetaData;
import org.jboss.metadata.javaee.spec.EnvironmentEntriesMetaData;
import org.jboss.metadata.javaee.spec.EnvironmentRefsGroupMetaData;
import org.jboss.metadata.javaee.spec.LifecycleCallbacksMetaData;
import org.jboss.metadata.javaee.spec.MessageDestinationReferencesMetaData;
import org.jboss.metadata.javaee.spec.PersistenceContextReferencesMetaData;
import org.jboss.metadata.javaee.spec.PersistenceUnitReferencesMetaData;
import org.jboss.metadata.javaee.spec.ResourceEnvironmentReferencesMetaData;
import org.jboss.metadata.javaee.spec.ResourceReferencesMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferencesMetaData;


/**
 * @author Remy Maucherat
 */
public class EnvironmentRefsGroupMetaDataParser {

    public static boolean parse(XMLStreamReader reader, EnvironmentRefsGroupMetaData env) throws XMLStreamException {
        // Only look at the current element, no iteration
        final Element element = Element.forName(reader.getLocalName());
        switch (element) {
            case ENV_ENTRY:
                EnvironmentEntriesMetaData envEntries = env.getEnvironmentEntries();
                if (envEntries == null) {
                    envEntries = new EnvironmentEntriesMetaData();
                    env.setEnvironmentEntries(envEntries);
                }
                envEntries.add(EnvironmentEntryMetaDataParser.parse(reader));
                break;
            case EJB_REF:
                EJBReferencesMetaData ejbReferences = env.getEjbReferences();
                if (ejbReferences == null) {
                    ejbReferences = new EJBReferencesMetaData();
                    env.setEjbReferences(ejbReferences);
                }
                ejbReferences.add(EJBReferenceMetaDataParser.parse(reader));
                break;
            case EJB_LOCAL_REF:
                EJBLocalReferencesMetaData ejbLocalReferences = env.getEjbLocalReferences();
                if (ejbLocalReferences == null) {
                    ejbLocalReferences = new EJBLocalReferencesMetaData();
                    env.setEjbLocalReferences(ejbLocalReferences);
                }
                ejbLocalReferences.add(EJBLocalReferenceMetaDataParser.parse(reader));
                break;
            case SERVICE_REF:
                ServiceReferencesMetaData serviceReferences = env.getServiceReferences();
                if (serviceReferences == null) {
                    serviceReferences = new ServiceReferencesMetaData();
                    env.setServiceReferences(serviceReferences);
                }
                serviceReferences.add(ServiceReferenceMetaDataParser.parse(reader));
                break;
            case RESOURCE_REF:
                ResourceReferencesMetaData resourceReferences = env.getResourceReferences();
                if (resourceReferences == null) {
                    resourceReferences = new ResourceReferencesMetaData();
                    env.setResourceReferences(resourceReferences);
                }
                resourceReferences.add(ResourceReferenceMetaDataParser.parse(reader));
                break;
            case RESOURCE_ENV_REF:
                ResourceEnvironmentReferencesMetaData resourceEnvReferences = env.getResourceEnvironmentReferences();
                if (resourceEnvReferences == null) {
                    resourceEnvReferences = new ResourceEnvironmentReferencesMetaData();
                    env.setResourceEnvironmentReferences(resourceEnvReferences);
                }
                resourceEnvReferences.add(ResourceEnvironmentReferenceMetaDataParser.parse(reader));
                break;
            case MESSAGE_DESTINATION_REF:
                MessageDestinationReferencesMetaData mdReferences = env.getMessageDestinationReferences();
                if (mdReferences == null) {
                    mdReferences = new MessageDestinationReferencesMetaData();
                    env.setMessageDestinationReferences(mdReferences);
                }
                mdReferences.add(MessageDestinationReferenceMetaDataParser.parse(reader));
                break;
            case PERSISTENCE_CONTEXT_REF:
                PersistenceContextReferencesMetaData pcReferences = env.getPersistenceContextRefs();
                if (pcReferences == null) {
                    pcReferences = new PersistenceContextReferencesMetaData();
                    env.setPersistenceContextRefs(pcReferences);
                }
                pcReferences.add(PersistenceContextReferenceMetaDataParser.parse(reader));
                break;
            case PERSISTENCE_UNIT_REF:
                PersistenceUnitReferencesMetaData puReferences = env.getPersistenceUnitRefs();
                if (puReferences == null) {
                    puReferences = new PersistenceUnitReferencesMetaData();
                    env.setPersistenceUnitRefs(puReferences);
                }
                puReferences.add(PersistenceUnitReferenceMetaDataParser.parse(reader));
                break;
            case POST_CONSTRUCT:
                LifecycleCallbacksMetaData postConstructs = env.getPostConstructs();
                if (postConstructs == null) {
                    postConstructs = new LifecycleCallbacksMetaData();
                    env.setPostConstructs(postConstructs);
                }
                postConstructs.add(LifecycleCallbackMetaDataParser.parse(reader));
                break;
            case PRE_DESTROY:
                LifecycleCallbacksMetaData preDestroys = env.getPreDestroys();
                if (preDestroys == null) {
                    preDestroys = new LifecycleCallbacksMetaData();
                    env.setPreDestroys(preDestroys);
                }
                preDestroys.add(LifecycleCallbackMetaDataParser.parse(reader));
                break;
            case DATA_SOURCE:
                DataSourcesMetaData dataSources = env.getDataSources();
                if (dataSources == null) {
                    dataSources = new DataSourcesMetaData();
                    env.setDataSources(dataSources);
                }
                dataSources.add(DataSourceMetaDataParser.parse(reader));
                break;
            default: return false;
        }
        return true;
    }

}
