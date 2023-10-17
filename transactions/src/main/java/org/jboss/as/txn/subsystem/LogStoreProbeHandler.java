/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.dmr.ModelNode;

import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handler for exposing transaction logs
 *
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a> (c) 2011 Red Hat Inc.
 * @author <a href="mmusgrove@redhat.com">Mike Musgrove</a> (c) 2012 Red Hat Inc.
 */
public class LogStoreProbeHandler implements OperationStepHandler {

    static final LogStoreProbeHandler INSTANCE = new LogStoreProbeHandler();
    static final String osMBeanName = "jboss.jta:type=ObjectStore";
    static final String JNDI_PROPNAME =
            LogStoreConstants.MODEL_TO_JMX_PARTICIPANT_NAMES.get(LogStoreConstants.JNDI_ATTRIBUTE);

    private Map<String, String> getMBeanValues(MBeanServerConnection cnx, ObjectName on, String ... attributeNames)
            throws InstanceNotFoundException, IOException, ReflectionException, IntrospectionException {

        if (attributeNames == null) {
            MBeanInfo info = cnx.getMBeanInfo( on );
            MBeanAttributeInfo[] attributeArray = info.getAttributes();
            int i = 0;
            attributeNames = new String[attributeArray.length];

            for (MBeanAttributeInfo ai : attributeArray)
                attributeNames[i++] = ai.getName();
        }

        AttributeList attributes = cnx.getAttributes(on, attributeNames);
        Map<String, String> values = new HashMap<String, String>();

        for (javax.management.Attribute attribute : attributes.asList()) {
            Object value = attribute.getValue();

            values.put(attribute.getName(), value == null ? "" : value.toString());
        }

        return values;
    }

    private void addAttributes(ModelNode node, Map<String, String> model2JmxNames, Map<String, String> attributes) {
        for (Map.Entry<String, String> e : model2JmxNames.entrySet()) {
            String attributeValue = attributes.get(e.getValue());

            if (attributeValue != null)
                node.get(e.getKey()).set(attributeValue);
        }
    }

    private void addParticipants(final Resource parent, Set<ObjectInstance> participants, MBeanServer mbs)
            throws IntrospectionException, InstanceNotFoundException, IOException, ReflectionException {
        int i = 1;

        for (ObjectInstance participant : participants) {
            final Resource resource = new LogStoreResource.LogStoreRuntimeResource(participant.getObjectName());
            final ModelNode model = resource.getModel();
            Map<String, String> pAttributes = getMBeanValues(mbs,  participant.getObjectName(),
                    LogStoreConstants.PARTICIPANT_JMX_NAMES);
            String pAddress = pAttributes.get(JNDI_PROPNAME);

            if (pAddress == null || pAddress.length() == 0) {
                pAttributes.put(JNDI_PROPNAME, String.valueOf(i++));
                pAddress = pAttributes.get(JNDI_PROPNAME);
            }

            addAttributes(model, LogStoreConstants.MODEL_TO_JMX_PARTICIPANT_NAMES, pAttributes);
            // model.get(LogStoreConstants.JMX_ON_ATTRIBUTE).set(participant.getObjectName().getCanonicalName());

            final PathElement element = PathElement.pathElement(LogStoreConstants.PARTICIPANTS, pAddress);
            parent.registerChild(element, resource);
        }
    }

    private void addTransactions(final Resource parent, Set<ObjectInstance> transactions, MBeanServer mbs)
            throws IntrospectionException, InstanceNotFoundException, IOException,
            ReflectionException, MalformedObjectNameException {

        for (ObjectInstance oi : transactions) {
            String transactionId = oi.getObjectName().getCanonicalName();

            if (!transactionId.contains("puid") && transactionId.contains("itype")) {
                final Resource transaction = new LogStoreResource.LogStoreRuntimeResource(oi.getObjectName());
                final ModelNode model = transaction.getModel();

                Map<String, String> tAttributes = getMBeanValues(
                        mbs,  oi.getObjectName(), LogStoreConstants.TXN_JMX_NAMES);
                String txnId = tAttributes.get("Id");

                addAttributes(model, LogStoreConstants.MODEL_TO_JMX_TXN_NAMES, tAttributes);
                // model.get(LogStoreConstants.JMX_ON_ATTRIBUTE).set(transactionId);

                String participantQuery =  transactionId + ",puid=*";
                Set<ObjectInstance> participants = mbs.queryMBeans(new ObjectName(participantQuery), null);

                addParticipants(transaction, participants, mbs);

                final PathElement element = PathElement.pathElement(LogStoreConstants.TRANSACTIONS, txnId);
                parent.registerChild(element, transaction);
            }
        }
    }

    private Resource probeTransactions(MBeanServer mbs, boolean exposeAllLogs)
            throws OperationFailedException {
        try {
            ObjectName on = new ObjectName(osMBeanName);

            mbs.setAttribute(on, new javax.management.Attribute("ExposeAllRecordsAsMBeans", Boolean.valueOf(exposeAllLogs)));
            mbs.invoke(on, "probe", null, null);

            Set<ObjectInstance> transactions = mbs.queryMBeans(new ObjectName(osMBeanName +  ",*"), null);

            final Resource resource = Resource.Factory.create();
            addTransactions(resource, transactions, mbs);
            return resource;

        } catch (JMException e) {
            throw TransactionLogger.ROOT_LOGGER.transactionDiscoveryError(e);
        } catch (IOException e) {
            throw TransactionLogger.ROOT_LOGGER.transactionDiscoveryError(e);
        }
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if(! context.isNormalServer()) {
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            return;
        }
        final MBeanServer mbs = TransactionExtension.getMBeanServer(context);
        if (mbs != null) {
            // Get the log-store resource
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            assert resource instanceof LogStoreResource;
            final LogStoreResource logStore = (LogStoreResource) resource;
            // Get the expose-all-logs parameter value
            final ModelNode subModel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
            final boolean exposeAllLogs = LogStoreConstants.EXPOSE_ALL_LOGS.resolveModelAttribute(context, subModel).asBoolean();
            final Resource storeModel = probeTransactions(mbs, exposeAllLogs);
            // Replace the current model with an updated one
            context.acquireControllerLock();
            // WFLY-3020 -- don't drop the root model
            storeModel.writeModel(logStore.getModel());
            logStore.update(storeModel);
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

}
