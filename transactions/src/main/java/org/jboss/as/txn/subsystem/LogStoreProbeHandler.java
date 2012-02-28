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

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

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

    private void removeTransactions(OperationContext context) {
        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);

        for (Resource resource1 : resource.getChildren(LogStoreConstants.TRANSACTIONS)) {
            String txid = resource1.getModel().get(LogStoreConstants.TRANSACTION_ID.getName()).asString();

            resource.removeChild(PathElement.pathElement(LogStoreConstants.TRANSACTIONS, txid));
        }
    }

    private void addAttributes(ModelNode node, Map<String, String> model2JmxNames, Map<String, String> attributes) {
        for (Map.Entry<String, String> e : model2JmxNames.entrySet()) {
            String attributeValue = attributes.get(e.getValue());

            if (attributeValue != null)
                node.get(e.getKey()).set(attributeValue);
        }
    }

    private void addParticipants(OperationContext context, ModelNode operation, ModelNode parent, Set<ObjectInstance> participants, MBeanServer mbs)
            throws IntrospectionException, InstanceNotFoundException, IOException, ReflectionException {
        int i = 1;

        for (ObjectInstance participant : participants) {
            final ModelNode participantAddress = parent.clone();
            final ModelNode participantOperation = new ModelNode();
            Map<String, String> pAttributes = getMBeanValues(mbs,  participant.getObjectName(),
                    LogStoreConstants.PARTICIPANT_JMX_NAMES);
            String pAddress = pAttributes.get(JNDI_PROPNAME);

            if (pAddress.length() == 0) {
                pAttributes.put(JNDI_PROPNAME, String.valueOf(i));
                pAddress = pAttributes.get(JNDI_PROPNAME);
            }

            operation.get(OP).set(ADD);

            participantAddress.add(LogStoreConstants.PARTICIPANTS, pAddress);

            participantOperation.get(OP_ADDR).set(participantAddress);
            addAttributes(participantOperation, LogStoreConstants.MODEL_TO_JMX_PARTICIPANT_NAMES, pAttributes);
            participantOperation.get(LogStoreConstants.JMX_ON_ATTRIBUTE).set(
                    participant.getObjectName().getCanonicalName());

            context.addStep(participantOperation, LogStoreParticipantAddHandler.INSTANCE, OperationContext.Stage.MODEL);
        }
    }

    private void addTransactions(
            OperationContext context, ModelNode operation, Set<ObjectInstance> transactions, MBeanServer mbs)
            throws IntrospectionException, InstanceNotFoundException, IOException,
            ReflectionException, MalformedObjectNameException {

        for (ObjectInstance oi : transactions) {
            String transactionId = oi.getObjectName().getCanonicalName();

            if (!transactionId.contains("puid") && transactionId.contains("itype")) {
                final ModelNode transactionAddress = operation.get("address").clone();
                final ModelNode transactionOperation = new ModelNode();

                Map<String, String> tAttributes = getMBeanValues(
                        mbs,  oi.getObjectName(), LogStoreConstants.TXN_JMX_NAMES);
                String txnId = tAttributes.get("Id");

                operation.get(OP).set(ADD);
                transactionAddress.add(LogStoreConstants.TRANSACTIONS, txnId);

                transactionOperation.get(OP_ADDR).set(transactionAddress);
                addAttributes(transactionOperation, LogStoreConstants.MODEL_TO_JMX_TXN_NAMES, tAttributes);
                transactionOperation.get(LogStoreConstants.JMX_ON_ATTRIBUTE).set(transactionId);

                context.addStep(
                        transactionOperation, LogStoreTransactionAddHandler.INSTANCE, OperationContext.Stage.MODEL);

                String participantQuery =  transactionId + ",puid=*";
                Set<ObjectInstance> participants = mbs.queryMBeans(new ObjectName(participantQuery), null);

                addParticipants(context, operation, transactionAddress, participants, mbs);
            }
        }
    }

    private void probeTransactions(OperationContext context, ModelNode operation, MBeanServer mbs)
            throws OperationFailedException {
        try {
            ObjectName on = new ObjectName(osMBeanName);

            mbs.invoke(on, "probe", null, null);

            Set<ObjectInstance> transactions = mbs.queryMBeans(new ObjectName(osMBeanName +  ",*"), null);

            removeTransactions(context);
            addTransactions(context, operation, transactions, mbs);

        } catch (JMException e) {
            throw new OperationFailedException("Transaction discovery error: ", e);
        } catch (IOException e) {
            throw new OperationFailedException("Transaction discovery error: ", e);
        }
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        MBeanServer mbs = TransactionExtension.getMBeanServer(context);

        if (mbs != null)
            probeTransactions(context, operation, mbs);

        context.completeStep();
    }
}
