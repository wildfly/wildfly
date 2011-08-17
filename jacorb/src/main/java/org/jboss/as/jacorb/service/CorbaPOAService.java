/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jacorb.service;

import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.IdUniquenessPolicyValue;
import org.omg.PortableServer.ImplicitActivationPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.omg.PortableServer.ThreadPolicyValue;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This class implements a service that creates and activates {@code org.omg.PortableServer.POA} objects.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class CorbaPOAService implements Service<POA> {

    private static final Logger log = Logger.getLogger("org.jboss.as.jacorb");

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jacorb", "poa-service");

    private volatile POA poa;

    private final InjectedValue<ORB> orbInjector = new InjectedValue<ORB>();

    private final InjectedValue<POA> parentPOAInjector = new InjectedValue<POA>();

    private final String poaName;

    private final String bindingName;

    // the policy values that can be assigned to created POAs.

    private final IdAssignmentPolicyValue idAssignmentPolicyValue;

    private final IdUniquenessPolicyValue idUniquenessPolicyValue;

    private final ImplicitActivationPolicyValue implicitActivationPolicyValue;

    private final LifespanPolicyValue lifespanPolicyValue;

    private final RequestProcessingPolicyValue requestProcessingPolicyValue;

    private final ServantRetentionPolicyValue servantRetentionPolicyValue;

    private final ThreadPolicyValue threadPolicyValue;

    /**
     * <p>
     * Creates a {@code CorbaPOAService} with the specified POA name and binding name. The {@code POA} created by this
     * service will not be associated with any policies.
     * </p>
     *
     * @param poaName     the name of the {@code POA} that will be created by this service (ex. "RootPOA").
     * @param bindingName the JNDI context name where the created {@code POA} will be bound. If null, the JNDI binding
     *                    won't be performed.
     */
    public CorbaPOAService(String poaName, String bindingName) {
        this(poaName, bindingName, null, null, null, null, null, null, null);
    }

    /**
     * <p>
     * Creates a {@code CorbaPOAService} with the specified POA name, binding name and policy values.
     * </p>
     *
     * @param poaName                       the name of the {@code POA} that will be created by this service (ex. "RootPOA").
     * @param bindingName                   the JNDI context name where the created {@code POA} will be bound. If null, the JNDI binding
     *                                      won't be performed.
     * @param idAssignmentPolicyValue       the {@code IdAssignmentPolicyValue} that will be associated with the created
     *                                      {@code POA}. Can be null.
     * @param idUniquenessPolicyValue       the {@code IdUniquenessPolicyValue} that will be associated with the created
     *                                      {@code POA}. Can be null.
     * @param implicitActivationPolicyValue the {@code ImplicitActivationPolicyValue} that will be associated with the
     *                                      created {@code POA}. Can be null.
     * @param lifespanPolicyValue           the {@code LifespanPolicyValue} that will be associated with the created {@code POA}.
     *                                      Can be null.
     * @param requestProcessingPolicyValue  the {@code RequestProcessingPolicyValue} that will be associated with the
     *                                      created {@code POA}. Can be null.
     * @param servantRetentionPolicyValue   the {@code ServantRetentionPolicyValue} that will be associated with the created
     *                                      {@code POA}. Can be null.
     * @param threadPolicyValue             the {@code ThreadPolicyValue} that will be associated with the created {@code POA}. Can
     *                                      be null.
     */
    public CorbaPOAService(String poaName, String bindingName, IdAssignmentPolicyValue idAssignmentPolicyValue,
                           IdUniquenessPolicyValue idUniquenessPolicyValue, ImplicitActivationPolicyValue implicitActivationPolicyValue,
                           LifespanPolicyValue lifespanPolicyValue, RequestProcessingPolicyValue requestProcessingPolicyValue,
                           ServantRetentionPolicyValue servantRetentionPolicyValue, ThreadPolicyValue threadPolicyValue) {
        this.poaName = poaName;
        this.bindingName = bindingName;
        this.idAssignmentPolicyValue = idAssignmentPolicyValue;
        this.idUniquenessPolicyValue = idUniquenessPolicyValue;
        this.implicitActivationPolicyValue = implicitActivationPolicyValue;
        this.lifespanPolicyValue = lifespanPolicyValue;
        this.requestProcessingPolicyValue = requestProcessingPolicyValue;
        this.servantRetentionPolicyValue = servantRetentionPolicyValue;
        this.threadPolicyValue = threadPolicyValue;
    }


    @Override
    public void start(StartContext context) throws StartException {
        log.debugf("Starting Service " + context.getController().getName().getCanonicalName());

        ORB orb = this.orbInjector.getOptionalValue();
        POA parentPOA = this.parentPOAInjector.getOptionalValue();

        // if an ORB has been injected, we will use the ORB.resolve_initial_references method to instantiate the POA.
        if (orb != null) {
            try {
                this.poa = POAHelper.narrow(orb.resolve_initial_references(this.poaName));
            } catch (Exception e) {
                throw new StartException("Failed to resolve initial reference " + this.poaName, e);
            }
        }
        // if a parent POA has been injected, we use it to create the policies and then the POA itself.
        else if (parentPOA != null) {
            try {
                Policy[] poaPolicies = this.createPolicies(parentPOA);
                this.poa = parentPOA.create_POA(this.poaName, null, poaPolicies);
            } catch (Exception e) {
                throw new StartException("Failed to create POA from parent POA", e);
            }
        } else {
            throw new StartException("Unable to instantiate POA: either the running ORB or the parent POA must be specified");
        }

        // check if the POA should be bound to JNDI under java:/jboss.
        if (this.bindingName != null) {
            CorbaServiceUtil.bindObject(context.getChildTarget(), this.bindingName, this.poa);
        }

        // activate the created POA.
        try {
            this.poa.the_POAManager().activate();
        } catch (Exception e) {
            throw new StartException("Failed to activate POA", e);
        }
    }

    @Override
    public void stop(StopContext context) {
        log.debugf("Stopping Service " + context.getController().getName().getCanonicalName());
        // destroy the created POA.
        this.poa.destroy(false, false);
    }

    @Override
    public POA getValue() throws IllegalStateException, IllegalArgumentException {
        return this.poa;
    }

    public Injector<ORB> getORBInjector() {
        return this.orbInjector;
    }

    public Injector<POA> getParentPOAInjector() {
        return this.parentPOAInjector;
    }

    /**
     * <p>
     * Create the {@code Policy} array containing the {@code POA} policies using the values specified in the constructor.
     * When creating a {@code POA}, the parent {@code POA} is responsible for generating the relevant policies beforehand.
     * </p>
     *
     * @param poa the {@code POA} used to create the {@code Policy} objects.
     * @return the constructed {@code Policy} array.
     */
    private Policy[] createPolicies(POA poa) {
        List<Policy> policies = new ArrayList<Policy>();

        if (this.idAssignmentPolicyValue != null)
            policies.add(poa.create_id_assignment_policy(this.idAssignmentPolicyValue));
        if (this.idUniquenessPolicyValue != null)
            policies.add(poa.create_id_uniqueness_policy(this.idUniquenessPolicyValue));
        if (this.implicitActivationPolicyValue != null)
            policies.add(poa.create_implicit_activation_policy(this.implicitActivationPolicyValue));
        if (this.lifespanPolicyValue != null)
            policies.add(poa.create_lifespan_policy(this.lifespanPolicyValue));
        if (this.requestProcessingPolicyValue != null)
            policies.add(poa.create_request_processing_policy(this.requestProcessingPolicyValue));
        if (this.servantRetentionPolicyValue != null)
            policies.add(poa.create_servant_retention_policy(this.servantRetentionPolicyValue));
        if (this.threadPolicyValue != null)
            policies.add(poa.create_thread_policy(this.threadPolicyValue));

        return policies.toArray(new Policy[policies.size()]);
    }
}
