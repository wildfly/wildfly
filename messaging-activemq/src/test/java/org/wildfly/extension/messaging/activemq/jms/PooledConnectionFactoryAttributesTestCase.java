package org.wildfly.extension.messaging.activemq.jms;

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.activemq.artemis.ra.ActiveMQResourceAdapter;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Pooled;
import org.junit.Test;

public class PooledConnectionFactoryAttributesTestCase extends AttributesTestBase{

    private static final SortedSet<String> UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES;
    private static final SortedSet<String> KNOWN_ATTRIBUTES;

    static {
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES = new TreeSet<String>();

        // we configure discovery group using discoveryGroupName instead of individual params:
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add(PooledConnectionFactoryService.GROUP_ADDRESS);
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add(PooledConnectionFactoryService.GROUP_PORT);
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add(PooledConnectionFactoryService.REFRESH_TIMEOUT);
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add(PooledConnectionFactoryService.DISCOVERY_LOCAL_BIND_ADDRESS);
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add(PooledConnectionFactoryService.DISCOVERY_INITIAL_WAIT_TIMEOUT);
        // these properties must not be exposed by the AS7 messaging subsystem
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add(PooledConnectionFactoryService.CONNECTION_PARAMETERS);
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add(PooledConnectionFactoryService.CONNECTOR_CLASSNAME);
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add("managedConnectionFactory");
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add(PooledConnectionFactoryService.JGROUPS_CHANNEL_LOCATOR_CLASS);
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add(PooledConnectionFactoryService.JGROUPS_CHANNEL_REF_NAME);
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add(PooledConnectionFactoryService.JGROUPS_CHANNEL_NAME);
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add("jgroupsFile");
        // these 2 props will *not* be supported since AS7 relies on vaulted passwords + expressions instead
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add("passwordCodec");
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add("useMaskedPassword");

        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add("connectionPoolName");
        UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES.add("cacheDestinations");

        KNOWN_ATTRIBUTES = new TreeSet<String>();
        // these are supported but it is not found by JavaBeans introspector because of the type
        // difference b/w the getter and the setters (Long vs long)
        KNOWN_ATTRIBUTES.add(Pooled.SETUP_ATTEMPTS_PROP_NAME);
        KNOWN_ATTRIBUTES.add(Pooled.SETUP_INTERVAL_PROP_NAME);
        KNOWN_ATTRIBUTES.add(Pooled.USE_JNDI_PROP_NAME);
        KNOWN_ATTRIBUTES.add(Pooled.REBALANCE_CONNECTIONS_PROP_NAME);
        KNOWN_ATTRIBUTES.add(Pooled.ALLOW_LOCAL_TRANSACTIONS_PROP_NAME);
    }

    @Test
    public void compareWildFlyPooledConnectionFactoryAndActiveMQConnectionFactoryProperties() throws Exception {
        SortedSet<String> pooledConnectionFactoryAttributes = findAllResourceAdapterProperties(PooledConnectionFactoryDefinition.ATTRIBUTES);
        pooledConnectionFactoryAttributes.removeAll(KNOWN_ATTRIBUTES);

        SortedSet<String> activemqRAProperties = findAllPropertyNames(ActiveMQResourceAdapter.class);
        activemqRAProperties.removeAll(UNSUPPORTED_ACTIVEMQ_RA_PROPERTIES);

        compare("AS7 PooledConnectionFactoryAttributes", pooledConnectionFactoryAttributes,
                "ActiveMQ Resource Adapter", activemqRAProperties);
    }

    private static SortedSet<String> findAllResourceAdapterProperties(ConnectionFactoryAttribute... attrs) {
        SortedSet<String> names = new TreeSet<String>();
        for (ConnectionFactoryAttribute attr : attrs) {
            if (attr.isResourceAdapterProperty()) {
                names.add(attr.getPropertyName());
            }
        }
        return names;
    }
}
