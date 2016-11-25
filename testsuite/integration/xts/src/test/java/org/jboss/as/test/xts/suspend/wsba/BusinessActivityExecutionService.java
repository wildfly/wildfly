/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.xts.suspend.wsba;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import com.arjuna.mw.wst11.BusinessActivityManager;
import com.arjuna.mw.wst11.UserBusinessActivity;
import com.arjuna.mwlabs.wst11.ba.remote.UserBusinessActivityImple;
import com.arjuna.wst11.BAParticipantManager;
import org.jboss.as.test.xts.suspend.ExecutorService;
import org.jboss.as.test.xts.suspend.RemoteService;
import org.jboss.jbossts.xts.environment.XTSPropertyManager;
import org.jboss.logging.Logger;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.mw.wst.TxContext;

import static org.jboss.as.test.xts.suspend.Helpers.getRemoteService;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@WebService(targetNamespace = "org.jboss.as.test.xts.suspend", serviceName = "ExecutorService", portName = "ExecutorService")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@HandlerChain(file = "/context-handlers.xml")
public class BusinessActivityExecutionService implements ExecutorService {

    private static final Logger LOGGER = Logger.getLogger(BusinessActivityExecutionService.class);

    private RemoteService remoteService;

    private TxContext currentActivity;

    private boolean wasInitialised;

    @Override
    public void init(String activationServiceUrl, String remoteServiceUrl) {
        LOGGER.infof("initialising with activationServiceUrl=%s and remoteServiceUrl=%s", activationServiceUrl,
                remoteServiceUrl);

        if (!wasInitialised) {
            // This is done only for testing purposes. In real application application server configuration should be used.
            XTSPropertyManager.getWSCEnvironmentBean().setCoordinatorURL11(activationServiceUrl);
            UserBusinessActivity.setUserBusinessActivity(new UserBusinessActivityImple());
            wasInitialised = true;
        }

        try {
            remoteService = getRemoteService(new URL(remoteServiceUrl));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void begin() throws Exception {
        assert currentActivity == null : "Business activity already started";
        LOGGER.infof("trying to begin transaction on %s", XTSPropertyManager.getWSCEnvironmentBean().getCoordinatorURL11());

        UserBusinessActivity.getUserBusinessActivity().begin();
        currentActivity = BusinessActivityManager.getBusinessActivityManager().suspend();

        LOGGER.infof("started business activity %s", currentActivity);
    }

    @Override
    public void commit() throws Exception {
        assert currentActivity != null : "No active business activity";
        LOGGER.infof("trying to close business activity %s", currentActivity);

        BusinessActivityManager.getBusinessActivityManager().resume(currentActivity);
        UserBusinessActivity.getUserBusinessActivity().close();
        currentActivity = null;

        LOGGER.infof("closed business activity");
    }

    @Override
    public void rollback() throws Exception {
        assert currentActivity != null : "No active business activity";
        LOGGER.infof("trying to cancel business activity %s", currentActivity);

        BusinessActivityManager.getBusinessActivityManager().resume(currentActivity);
        UserBusinessActivity.getUserBusinessActivity().cancel();
        currentActivity = null;

        LOGGER.infof("canceled business activity");
    }

    @Override
    public void enlistParticipant() throws Exception {
        assert currentActivity != null : "No active business activity";
        LOGGER.infof("trying to enlist participant to the business activity %s", currentActivity);

        BusinessActivityManager.getBusinessActivityManager().resume(currentActivity);
        BusinessActivityParticipant businessActivityParticipant = new BusinessActivityParticipant(new Uid().stringForm());
        BAParticipantManager participantManager = BusinessActivityManager.getBusinessActivityManager()
                .enlistForBusinessAgreementWithParticipantCompletion(businessActivityParticipant,
                        businessActivityParticipant.getId());
        participantManager.completed();
        currentActivity = BusinessActivityManager.getBusinessActivityManager().suspend();

        LOGGER.infof("enlisted participant %s", businessActivityParticipant);
    }

    @Override
    public void execute() throws Exception {
        assert remoteService != null : "Remote service was not initialised";
        assert currentActivity != null : "No active business activity";
        LOGGER.infof("trying to execute remote service in business activity %s", currentActivity);

        BusinessActivityManager.getBusinessActivityManager().resume(currentActivity);
        remoteService.execute();
        currentActivity = BusinessActivityManager.getBusinessActivityManager().suspend();

        LOGGER.infof("executed remote service");
    }

    @Override
    public void reset() {
        BusinessActivityParticipant.resetInvocations();
    }

    @Override
    public List<String> getParticipantInvocations() {
        return BusinessActivityParticipant.getInvocations();
    }
}
