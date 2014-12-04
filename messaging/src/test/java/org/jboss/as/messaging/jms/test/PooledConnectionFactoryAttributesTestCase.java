package org.jboss.as.messaging.jms.test;

import java.util.SortedSet;
import java.util.TreeSet;

import org.hornetq.ra.HornetQResourceAdapter;
import org.jboss.as.messaging.jms.ConnectionFactoryAttribute;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryService;
import org.junit.Test;

public class PooledConnectionFactoryAttributesTestCase extends AttributesTestBase{

    private static final SortedSet<String> UNSUPPORTED_HORNETQ_RA_PROPERTIES;
    private static final SortedSet<String> KNOWN_ATTRIBUTES;

    static {
        UNSUPPORTED_HORNETQ_RA_PROPERTIES = new TreeSet<String>();

        // we configure discovery group using discoveryGroupName instead of individual params:
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.GROUP_ADDRESS.toLowerCase());
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.GROUP_PORT.toLowerCase());
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.REFRESH_TIMEOUT.toLowerCase());
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.DISCOVERY_LOCAL_BIND_ADDRESS.toLowerCase());
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.DISCOVERY_INITIAL_WAIT_TIMEOUT.toLowerCase());
        // these properties must not be exposed by the AS7 messaging subsystem
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.CONNECTION_PARAMETERS.toLowerCase());
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.CONNECTOR_CLASSNAME.toLowerCase());
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.TRANSACTION_MANAGER_LOCATOR_CLASS.toLowerCase());
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.TRANSACTION_MANAGER_LOCATOR_METHOD.toLowerCase());
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add("managedConnectionFactory".toLowerCase());
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.JGROUPS_CHANNEL_LOCATOR_CLASS.toLowerCase());
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.JGROUPS_CHANNEL_REF_NAME.toLowerCase());
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add(PooledConnectionFactoryService.JGROUPS_CHANNEL_NAME.toLowerCase());
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add("jgroupsFile".toLowerCase());
        // these 2 props will *not* be supported since AS7 relies on vaulted passwords + expressions instead
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add("passwordCodec".toLowerCase());
        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add("useMaskedPassword".toLowerCase());

        UNSUPPORTED_HORNETQ_RA_PROPERTIES.add("connectionPoolName".toLowerCase());

        KNOWN_ATTRIBUTES = new TreeSet<String>();
        // these are supported but it is not found by JavaBeans introspector because of the type
        // difference b/w the getter and the setters (Long vs long)
        /*
        KNOWN_ATTRIBUTES.add(Pooled.SETUP_ATTEMPTS_PROP_NAME.toLowerCase());
        KNOWN_ATTRIBUTES.add(Pooled.SETUP_INTERVAL_PROP_NAME.toLowerCase());
        KNOWN_ATTRIBUTES.add(Pooled.USE_JNDI_PROP_NAME.toLowerCase());
        */
    }

    @Test
    public void compareAS7PooledConnectionFactoryAttributesAndHornetQConnectionFactoryProperties() throws Exception {
        SortedSet<String> pooledConnectionFactoryAttributes = findAllResourceAdapterProperties(PooledConnectionFactoryDefinition.ATTRIBUTES);
        pooledConnectionFactoryAttributes.removeAll(KNOWN_ATTRIBUTES);

        SortedSet<String> hornetQRAProperties = findAllPropertyNames(HornetQResourceAdapter.class);
        for (String name : UNSUPPORTED_HORNETQ_RA_PROPERTIES) {
            hornetQRAProperties.remove(name);
        }
        SortedSet<String> lowerCaseConfigurationProperties = new TreeSet<>();
        for (String name : hornetQRAProperties) {
            lowerCaseConfigurationProperties.add(name.toLowerCase());
        }

        compare("AS7 PooledConnectionFactoryAttributes", pooledConnectionFactoryAttributes,
                "HornetQ Resource Adapter", hornetQRAProperties);
    }

    private static final SortedSet<String> findAllResourceAdapterProperties(ConnectionFactoryAttribute... attrs) {
        SortedSet<String> names = new TreeSet<String>();
        for (ConnectionFactoryAttribute attr : attrs) {
            if (attr.isResourceAdapterProperty()) {
                names.add(attr.getPropertyName().toLowerCase());
            }
        }
        return names;
    }
}
