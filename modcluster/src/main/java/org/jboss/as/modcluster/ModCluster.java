package org.jboss.as.modcluster;

import java.net.InetSocketAddress;
import java.util.Map;

public interface ModCluster {
    Map<InetSocketAddress, String> getProxyInfo();
}
