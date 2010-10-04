package org.jboss.as.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hornetq.api.core.Pair;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.core.config.BridgeConfiguration;
import org.hornetq.core.config.BroadcastGroupConfiguration;
import org.hornetq.core.config.ClusterConnectionConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.DiscoveryGroupConfiguration;
import org.hornetq.core.config.DivertConfiguration;
import org.hornetq.core.config.CoreQueueConfiguration;
import org.hornetq.core.config.ConnectorServiceConfiguration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.config.impl.FileConfiguration;
import org.hornetq.core.config.impl.Validators;
import org.hornetq.core.journal.impl.AIOSequentialFileFactory;
import org.hornetq.core.logging.Logger;
import org.hornetq.core.security.Role;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.server.group.impl.GroupingHandlerConfiguration;
import org.hornetq.core.settings.impl.AddressFullMessagePolicy;
import org.hornetq.core.settings.impl.AddressSettings;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class ConfigReader {
}
