package org.jboss.as.test.clustering.cluster.service.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.Clustered;
import org.wildfly.clustering.Node;
import org.wildfly.clustering.service.ServiceProviderRegistration;

@Stateless
@Clustered
@Remote(ServiceProviderRetriever.class)
public class ServiceProviderRetrieverBean implements ServiceProviderRetriever {

    @EJB
    private ServiceProviderRegistration registration;
    
    @Override
    public Collection<String> getProviders() {
        Set<Node> nodes = this.registration.getServiceProviders();
        List<String> result = new ArrayList<>(nodes.size());
        for (Node node: nodes) {
            result.add(node.getName());
        }
        return result;
    }
}
