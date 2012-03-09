package org.jboss.as.modcluster;

import java.net.InetSocketAddress;
import java.util.Map;

public interface ModCluster {
    Map<InetSocketAddress, String> getProxyInfo();

    Map<InetSocketAddress, String> getProxyConfiguration();

    void refresh();

    boolean enableContext(String webhost, String webcontext);

    boolean disableContext(String webhost, String webcontext);

    boolean stopContext(String webhost, String webcontext, int waittime);

    void reset();

    void enable();

    void disable();

    void stop(int waittime);

    void addProxy(String webhost, int port);

    void removeProxy(String webhost, int port);
}
