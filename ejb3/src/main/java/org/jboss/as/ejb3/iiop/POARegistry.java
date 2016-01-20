/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.iiop;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.omg.CORBA.Policy;
import org.omg.CORBA.SetOverrideType;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.IdUniquenessPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantRetentionPolicyValue;

/**
 * Registry that maintains 2 different servant registries
 * <p/>
 * <ul>
 * <li>a <code>ServantRegistry</code> with a transient POA per servant;</li>
 * <li>a <code>ServantRegistry</code> with persistent POA per servant.</li>
 * </ul>
 * <p/>
 * CORBA servants registered with any of these
 * <code>ServantRegistry</code> instances will receive IIOP invocations.
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 */
public class POARegistry implements Service<POARegistry> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "iiop", "POARegistry");
    public static final byte[] EMPTY_BYTES = {};

    /**
     * The root POA. *
     */
    private final InjectedValue<POA> rootPOA = new InjectedValue<POA>();

    /**
     * A ServantRegistry with a transient POA per servant.
     */
    private ServantRegistry registryWithTransientPOAPerServant;

    /**
     * The transient POA map used by the ServantRegistry above.
     */
    private Map<String, POA> transientPoaMap;

    /**
     * POA policies used by the ServantRegistry above.
     */
    private Policy[] transientPoaPolicies;

    /**
     * A ServantRegistry with a persistent POA per servant.
     */
    private ServantRegistry registryWithPersistentPOAPerServant;

    /**
     * The persistent POA map used by the ServantRegistry above.
     */
    private Map<String, POA> persistentPoaMap;

    /**
     * POA policies used by the ServantRegistry above.
     */
    private Policy[] persistentPoaPolicies;

    public synchronized void start(final StartContext startContext) throws StartException {

        transientPoaMap = Collections.synchronizedMap(new HashMap<String, POA>());
        persistentPoaMap = Collections.synchronizedMap(new HashMap<String, POA>());
        final POA rootPOA = this.rootPOA.getValue();

        // Policies for per-servant transient POAs
        transientPoaPolicies = new Policy[]{rootPOA.create_lifespan_policy(
                LifespanPolicyValue.TRANSIENT),
                rootPOA.create_id_assignment_policy(IdAssignmentPolicyValue.SYSTEM_ID),
                rootPOA.create_servant_retention_policy(ServantRetentionPolicyValue.NON_RETAIN),
                rootPOA.create_request_processing_policy(RequestProcessingPolicyValue.USE_DEFAULT_SERVANT),
                rootPOA.create_id_uniqueness_policy( IdUniquenessPolicyValue.MULTIPLE_ID),
        };

        // Policies for per-servant persistent POAs
        persistentPoaPolicies = new Policy[]{
                rootPOA.create_lifespan_policy(
                        LifespanPolicyValue.PERSISTENT),
                rootPOA.create_id_assignment_policy(
                        IdAssignmentPolicyValue.USER_ID),
                rootPOA.create_servant_retention_policy(
                        ServantRetentionPolicyValue.NON_RETAIN),
                rootPOA.create_request_processing_policy(
                        RequestProcessingPolicyValue.USE_DEFAULT_SERVANT),
                rootPOA.create_id_uniqueness_policy(
                        IdUniquenessPolicyValue.MULTIPLE_ID),
        };

        // Create this POARegistry's ServantRegistry implementations
        registryWithTransientPOAPerServant = new ServantRegistryWithTransientPOAPerServant();
        registryWithPersistentPOAPerServant = new ServantRegistryWithPersistentPOAPerServant();
    }

    public synchronized void stop(final StopContext context) {
        transientPoaMap = null;
        persistentPoaMap = null;
        transientPoaPolicies = null;
        persistentPoaPolicies = null;
    }

    private static Policy[] concatPolicies(Policy[] policies1, Policy[] policies2) {
        Policy[] policies = new Policy[policies1.length + policies2.length];
        int j = 0;
        for (int i = 0; i < policies1.length; i++, j++) {
            policies[j] = policies1[i];
        }
        for (int i = 0; i < policies2.length; i++, j++) {
            policies[j] = policies2[i];
        }
        return policies;
    }

    @Override
    public synchronized POARegistry getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    static class PoaReferenceFactory implements ReferenceFactory {
        private final POA poa;
        private final Policy[] policies;
        PoaReferenceFactory(final POA poa, final Policy[] policies) {
            this.poa = poa;
            this.policies = policies;
        }

        PoaReferenceFactory(final POA poa) {
            this(poa, null);
        }

        public org.omg.CORBA.Object createReference(final String interfId) throws Exception {
            final org.omg.CORBA.Object corbaRef = poa.create_reference_with_id(EMPTY_BYTES, interfId);
            if (policies != null) {
                return corbaRef._set_policy_override(policies, SetOverrideType.ADD_OVERRIDE);
            } else {
                return corbaRef;
            }
        }

        public org.omg.CORBA.Object createReferenceWithId(final byte[] id, final String interfId) throws Exception {
            final org.omg.CORBA.Object corbaRef = poa.create_reference_with_id(id, interfId);
            if (policies != null) {
                return corbaRef._set_policy_override(policies, SetOverrideType.ADD_OVERRIDE);
            } else {
                return corbaRef;
            }
        }

        public POA getPOA() {
            return poa;
        }

    }

    /**
     * ServantRegistry with a transient POA per servant
     */
    class ServantRegistryWithTransientPOAPerServant implements ServantRegistry {

        public ReferenceFactory bind(final String name, final Servant servant, final Policy[] policies) throws Exception {
            final Policy[] poaPolicies = concatPolicies(transientPoaPolicies, policies);
            final POA poa = rootPOA.getValue().create_POA(name, null, poaPolicies);
            transientPoaMap.put(name, poa);
            poa.set_servant(servant);
            poa.the_POAManager().activate();
            return new PoaReferenceFactory(poa); // no servantName: in this case
            // name is the POA name
        }

        public ReferenceFactory bind(final String name, final Servant servant) throws Exception {
            final POA poa = rootPOA.getValue().create_POA(name, null, transientPoaPolicies);
            transientPoaMap.put(name, poa);
            poa.set_servant(servant);
            poa.the_POAManager().activate();
            return new PoaReferenceFactory(poa); // no servantName: in this case
            // name is the POA name
        }

        public void unbind(final String name) throws Exception {
            final POA poa = transientPoaMap.remove(name);
            if (poa != null) {
                poa.the_POAManager().deactivate(false, true);
                poa.destroy(false, true);
            }
        }

    }

    /**
     * ServantRegistry with a persistent POA per servant
     */
    class ServantRegistryWithPersistentPOAPerServant implements ServantRegistry {

        public ReferenceFactory bind(final String name, final Servant servant, final Policy[] policies) throws Exception {
            final Policy[] poaPolicies = concatPolicies(persistentPoaPolicies, policies);
            final POA poa = rootPOA.getValue().create_POA(name, null, poaPolicies);
            persistentPoaMap.put(name, poa);
            poa.set_servant(servant);
            poa.the_POAManager().activate();
            return new PoaReferenceFactory(poa); // no servantName: in this case
            // name is the POA name
        }

        public ReferenceFactory bind(final String name, final Servant servant) throws Exception {
            final POA poa = rootPOA.getValue().create_POA(name, null, persistentPoaPolicies);
            persistentPoaMap.put(name, poa);
            poa.set_servant(servant);
            poa.the_POAManager().activate();
            return new PoaReferenceFactory(poa); // no servantName: in this case
            // name is the POA name
        }

        public void unbind(final String name) throws Exception {
            final POA poa = persistentPoaMap.remove(name);
            if (poa != null) {
                poa.the_POAManager().deactivate(false, true);
                poa.destroy(false, true);
            }
        }

    }

    public ServantRegistry getRegistryWithTransientPOAPerServant() {
        return registryWithTransientPOAPerServant;
    }

    public ServantRegistry getRegistryWithPersistentPOAPerServant() {
        return registryWithPersistentPOAPerServant;
    }

    public InjectedValue<POA> getRootPOA() {
        return rootPOA;
    }
}
