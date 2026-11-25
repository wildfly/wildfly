/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.provider.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderRegistration;

@Stateless
@Remote(ServiceProviderRetriever.class)
public class ServiceProviderRetrieverBean implements ServiceProviderRetriever {

    @EJB
    private ServiceProviderRegistration<String, GroupMember> registration;

    @Override
    public Collection<String> getProviders() {
        Set<GroupMember> members = this.registration.getProviders();
        List<String> result = new ArrayList<>(members.size());
        for (GroupMember member: members) {
            result.add(member.getName());
        }
        return result;
    }
}
