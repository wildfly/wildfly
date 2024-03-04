/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.contextdata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

public class UseCaseValidator implements Serializable {

    private final List<UseCase> useCases = new ArrayList<>();
    private final Interface iface;

    public UseCaseValidator(Interface iface) {
        this.iface = iface;
        init();
    }

    public List<UseCase> getUseCases() {
        return this.useCases;
    }

    public static enum Interface {
        LOCAL, REMOTE
    }

    public static enum InvocationPhase {
        CLIENT_INT_HANDLE_INVOCATION, SERVER_INT_BEFORE, SERVER_EJB_INVOKE, SERVER_INT_AFTER, CLIENT_INT_HANDLE_INVOCATION_RESULT
    }

    private void init() {

        // *) We should probably clear the entire ContextData before the client unmarshalls the response, but we are going to try
        // this upstream since we do not wnat to potentially break some customer in EAP 7
        // - we also need to make sure there is no CDI stuff that needs to keep it.
        // Document special keys, like jboss.returned.keys, jboss.source.address, etc Make sure these are in

        // Use Case 1 - client interceptor sends data to EJB , but it is not sent back
        useCases.add(new UseCase(1, "client interceptor sends data to EJB , but it is not sent back") {
            @Override
            public void init() {
                // add data to UseCase1 key in the client interceptor and expect to see it until the server interceptor finishes
                // we should not see it in the client inteceptor result handle, as we do not include it in the jboss.return.keys

                // TODO it looks like the local interface sents back the data even if jboss.returned.keys is not set, this is a hack to handle the difference

                String serverUpdatedValue = getSimpleData("server-updated");

                // we will set the value on the client side and expect it to never change
                if(iface == Interface.REMOTE) {
                    super.addExpectation(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION, InvocationPhase.SERVER_INT_AFTER);
                } else { // local interface is getting the returned value since it is inVM, so we will expect it to change
                    super.addExpectation(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION, InvocationPhase.SERVER_INT_AFTER);
                    super.addExpectation(InvocationPhase.SERVER_INT_AFTER, serverUpdatedValue);
                }

                super.putData(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION);

                // TODO since the client side does not clear out all keys, it will still see the original value sent by the client
                // in the future we should see if we can clear all keys out before unmarshalling the response
                // until then, this test will change the value on the server side to validate the new value is not sent back, we will
                // just expect the original value

                super.putData(InvocationPhase.SERVER_INT_AFTER,  serverUpdatedValue);
            }
        });

        getUseCases().add(new UseCase(2, "client interceptor sends data to EJB, EJB sees it and sends back") {
            @Override
            public void init() {
                // add data to UseCase1 key in the client interceptor and expect to see it until the server interceptor finishes
                // we should not see it in the client inteceptor result handle, as we do not include it in the jboss.return.keys

                // we want this key / value sent back from the server
                super.addReturnKey(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION);

                // put a value in the client side to send to the server
                super.addExpectation(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION, InvocationPhase.SERVER_INT_AFTER);
                super.putData(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION);

                // change the value and expectation to a different value to make sure the value was sent back
                String updated = getSimpleData("server-updated");
                super.putData(InvocationPhase.SERVER_INT_AFTER, updated);
                super.addExpectation(InvocationPhase.SERVER_INT_AFTER, updated);

                // addExpectation (adds expectation to start after , and removes the expectation after phase has been checked)
                // putData - adds it to the ContextData in the given phase - this happens after testing
            }
        });

        // 3) client interceptor sends data to EJB, EJB modifies and sends back
        getUseCases().add(new UseCase(3, "client interceptor sends data to EJB, EJB modifies and sends back") {
            @Override
            public void init() {
                // we want this key / value sent back from the server
                super.addReturnKey(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION);

                // put a value in the client side to send to the server
                super.addExpectation(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION, InvocationPhase.SERVER_EJB_INVOKE);
                super.putData(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION);

                // change the value and expectation to a different value to make sure the value was sent back
                String updated = getSimpleData("server-updated");
                super.putData(InvocationPhase.SERVER_EJB_INVOKE, updated);
                super.addExpectation(InvocationPhase.SERVER_EJB_INVOKE, updated);
            }
        });

        // 4) client interceptor sends data to EJB, EJB removes it and client sees it removed
        getUseCases().add(new UseCase(4, "client interceptor sends data to EJB, EJB removes it and client sees it removed") {
            @Override
            public void init() {
                // we want this key / value sent back from the server
                super.addReturnKey(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION);

                // we will send a set of size 2 and the ejb will remove 1 and send it back
                Set<String> data = new HashSet<>();
                data.add("A");
                data.add("B");

                // put a value in the client side to send to the server
                super.addExpectation(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION, InvocationPhase.SERVER_EJB_INVOKE, data);
                super.putData(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION, data);

                // remove B when the EJB invokes
                super.addPhaseContextDataConsumer(InvocationPhase.SERVER_EJB_INVOKE, (Consumer<Map<String,Object>> & Serializable) o -> ((Set<String>)o.get(this.getContextDataUseCaseKey())).remove("B") );
                // change the expectation to match
                Set<String> updated = new HashSet<>();
                updated.add("A");
                super.addExpectation(InvocationPhase.SERVER_EJB_INVOKE, updated);
            }
        });

        // 5) client interceptor sends data to EJB, EJB removes it and client sees that the value is null as it was not returned
        getUseCases().add(new UseCase(5, "client interceptor sends data to EJB, EJB removes it and client sees that the value is null as it was not returned") {
            @Override
            public void init() {
                // we want this key / value sent back from the server
                super.addReturnKey(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION);

                // we will send a set of size 2 and the ejb will remove 1 and send it back
                Set<String> data = new HashSet<>();
                data.add("A");
                data.add("B");

                // put a value in the client side to send to the server
                super.addExpectation(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION, InvocationPhase.SERVER_EJB_INVOKE, data);
                super.putData(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION, data);

                // remove the data, and set the expectation that it is not returned
                super.removeData(InvocationPhase.SERVER_EJB_INVOKE);
                // change the expectation to match , expect null
                super.removeExpectation(InvocationPhase.SERVER_EJB_INVOKE);
            }
        });

        // EJB Interceptor setting data:
        // 6) EJB Interceptor sets data, EJB sees it
        getUseCases().add(new UseCase(6, "EJB Interceptor sets data, EJB sees it") {
            @Override
            public void init() {

                // put a value in the ejb interceptor
                // it will not be returned, so we should see it from Server Interceptor Before -> EJB -> Server Interceptor After for Remote
                if(iface == Interface.REMOTE)
                    super.addExpectation(InvocationPhase.SERVER_INT_BEFORE, InvocationPhase.SERVER_INT_AFTER);
                else // local sees it on the return
                    super.addExpectation(InvocationPhase.SERVER_INT_BEFORE, InvocationPhase.CLIENT_INT_HANDLE_INVOCATION_RESULT);

                super.putData(InvocationPhase.SERVER_INT_BEFORE);
            }
        });

        // 7) EJB Interceptor sets data, EJB sees it, client sees it sent back
        getUseCases().add(new UseCase(7, "EJB Interceptor sets data, EJB sees it, client sees it sent back") {
            @Override
            public void init() {

                // put a value in the ejb interceptor
                // this is same as 5 , except both will see it sent back since we set the jboss.returned.keys
                // the return keys has to be set on the client side before making the call
                super.addReturnKey(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION);

                super.addExpectation(InvocationPhase.SERVER_INT_BEFORE, InvocationPhase.CLIENT_INT_HANDLE_INVOCATION_RESULT);
                super.putData(InvocationPhase.SERVER_INT_BEFORE);
            }
        });

        // 8) EJB Interceptor sets data, EJB sees it, EJB modifies it, client sees it sent back
        getUseCases().add(new UseCase(8, "EJB Interceptor sets data, EJB sees it, EJB modifies it, client sees it sent back") {
            @Override
            public void init() {

                // put a value in the ejb interceptor
                // the return keys has to be set on the client side before making the call
                super.addReturnKey(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION);

                super.addExpectation(InvocationPhase.SERVER_INT_BEFORE, InvocationPhase.SERVER_EJB_INVOKE);
                super.putData(InvocationPhase.SERVER_INT_BEFORE);

                String updated = getSimpleData("server-updated");
                super.addExpectation(InvocationPhase.SERVER_EJB_INVOKE, InvocationPhase.CLIENT_INT_HANDLE_INVOCATION_RESULT, updated);
                super.putData(InvocationPhase.SERVER_EJB_INVOKE, updated);
            }
        });

        // 9) EJB Interceptor sets data, EJB sees it, EJB removes it, client sees it removed
        getUseCases().add(new UseCase(9, "EJB Interceptor sets data, EJB sees it, EJB removes it, client sees it removed") {
            @Override
            public void init() {

                // put a value in the ejb interceptor
                // the return keys has to be set on the client side before making the call
                super.addReturnKey(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION);

                super.addExpectation(InvocationPhase.SERVER_INT_BEFORE, InvocationPhase.SERVER_EJB_INVOKE);
                super.putData(InvocationPhase.SERVER_INT_BEFORE);

                super.removeData(InvocationPhase.SERVER_EJB_INVOKE);
                super.addExpectation(InvocationPhase.SERVER_EJB_INVOKE, InvocationPhase.CLIENT_INT_HANDLE_INVOCATION_RESULT, null);
            }
        });


        // 10) EJB Interceptor sets data after EJB call, EJB does not it, client sees it
        getUseCases().add(new UseCase(10, "EJB Interceptor sets data after EJB call, EJB does not it, client sees it") {
            @Override
            public void init() {

                // put a value in the ejb interceptor
                // the return keys has to be set on the client side before making the call
                super.addReturnKey(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION);

                super.addExpectation(InvocationPhase.SERVER_INT_AFTER, InvocationPhase.CLIENT_INT_HANDLE_INVOCATION_RESULT);
                super.putData(InvocationPhase.SERVER_INT_AFTER);
            }
        });


        // EJB Setting data:
        // 11) EJB sends data, EJB interceptor sees it at end, client interceptor does not it
        getUseCases().add(new UseCase(11, "EJB sends data, EJB interceptor sees it at end, client interceptor does not it") {
            @Override
            public void init() {

                // put a value in the ejb
                // since local sees values without jboss.returned.keys being set, we set the expectations differently
                if(iface == Interface.REMOTE)
                    super.addExpectation(InvocationPhase.SERVER_EJB_INVOKE, InvocationPhase.SERVER_INT_AFTER);
                else
                    super.addExpectation(InvocationPhase.SERVER_EJB_INVOKE, InvocationPhase.CLIENT_INT_HANDLE_INVOCATION_RESULT);

                super.putData(InvocationPhase.SERVER_EJB_INVOKE);
            }
        });

        // 12) EJB sends data, EJB interceptor sees it at end, client interceptor sees it
        getUseCases().add(new UseCase(12, "EJB sends data, EJB interceptor sees it at end, client interceptor sees it") {
            @Override
            public void init() {

                // same as 10 except both remote & local will see the same as jboss.returned.keys will be set
                // the return keys has to be set on the client side before making the call
                super.addReturnKey(InvocationPhase.CLIENT_INT_HANDLE_INVOCATION);

                // put a value in the ejb
                super.addExpectation(InvocationPhase.SERVER_EJB_INVOKE, InvocationPhase.CLIENT_INT_HANDLE_INVOCATION_RESULT);
                super.putData(InvocationPhase.SERVER_EJB_INVOKE);
            }
        });
    }

    public void test(InvocationPhase phase, Map<String, Object> contextData) throws TestException {
        for(UseCase uc: getUseCases())
            uc.test(phase, contextData);
    }

    public abstract static class UseCase implements Serializable {

        protected Logger logger;

        protected Map<InvocationPhase, Object> putDataAtPhase = new HashMap<>();
        protected Map<InvocationPhase, Object> removeDataAtPhase = new HashMap<>();
        protected Map<InvocationPhase, Object> addExpectationAtPhase = new HashMap<>();
        protected Map<InvocationPhase, String> addReturnKeyAtPhase = new HashMap<>();
        protected Set<InvocationPhase> removeExpectationAtPhase = new HashSet<>();
        protected Map<InvocationPhase, Consumer<Map<String,Object>>> phaseContextDataConsumer = new HashMap<>();
        protected int useCaseNumber;
        protected String description;
        protected String contextDataUseCaseKey;
        protected Object expected;

        public static final String RETURNED_CONTEXT_DATA_KEY = "jboss.returned.keys";

        public UseCase(int useCaseNumber, String description) {
            this.useCaseNumber = useCaseNumber;
            this.description = description;
            this.contextDataUseCaseKey = String.format("UseCase-%d", useCaseNumber);
            this.logger = Logger.getLogger(String.format("%s-%d", UseCase.class.getName(), useCaseNumber));
            init();
        }

        public abstract void init();

        public String getContextDataUseCaseKey() {
            return this.contextDataUseCaseKey;
        }

        private static boolean equal(Object o1, Object o2) {
            if(o1 != null)
                return o1.equals(o2);
            if(o2 != null)
                return o2.equals(o1);
            return o1 == o2;
        }

        // test will text expected matches
        // the actual value must always match the expected value else an exception will be thrown
        // when an expectation is removed, it is null and it is expected the actual value will also be null
        public void test(InvocationPhase phase, Map<String, Object> contextData) throws TestException {

            // expectations get tested first
            // changes to the expectations happen after the test
            // so we need to have a before / after

            contextData.keySet().forEach(k ->
                    logger.debugf("%s: ContextData %s -> %s", phase, k, contextData.get(k)));

            // test first before making any changes to expectations
            logger.debugf("phase: %s Test the current expectation: expected: %s contextDataUseCaseKey: %s value: %s", phase, expected, contextDataUseCaseKey, contextData.get(contextDataUseCaseKey));
            // Test the current expectation
            if(!equal(expected, contextData.get(contextDataUseCaseKey)))
                throw new TestException(String.format("%s : phase: %s expected: %s != actual: %s", this, phase, expected, contextData.get(contextDataUseCaseKey)));


            // remove the expected
            if (this.removeExpectationAtPhase.contains(phase)) {
                logger.debugf("phase: %s removeExpectationAtPhase %s expected: %s", phase, this.removeExpectationAtPhase, expected);
                this.expected = null;
            }

            // in the given phase add or remove an expectation
            if(this.addExpectationAtPhase.containsKey(phase)) {
                logger.debugf("phase: %s addExpectationAtPhase %s", phase, this.addExpectationAtPhase.get(phase));
                this.expected = this.addExpectationAtPhase.get(phase);
            }

            // in the given phase put the data
            if(this.removeDataAtPhase.containsKey(phase)) {
                logger.debugf("phase: %s removeDataAtPhase %s", phase, this.removeDataAtPhase.get(phase));
                contextData.remove(this.removeDataAtPhase.get(phase));
            }

            // in the given phase put the data
            if(this.putDataAtPhase.containsKey(phase)) {
                logger.debugf("phase: %s putDataAtPhase %s", phase, this.putDataAtPhase.get(phase));
                put(contextData, this.putDataAtPhase.get(phase));
            }

            // add any keys to jboss.returned.keys
            if(this.addReturnKeyAtPhase.containsKey(phase)) {
                logger.debugf("phase: %s addReturnKeyAtPhase %s", phase, addReturnKeyAtPhase);
                Set<String> jbossReturnedKeys = (Set<String>) contextData.get(RETURNED_CONTEXT_DATA_KEY);
                if(jbossReturnedKeys == null) {
                    jbossReturnedKeys = new HashSet<String>();
                    contextData.put(RETURNED_CONTEXT_DATA_KEY, jbossReturnedKeys);
                }
                jbossReturnedKeys.add(this.addReturnKeyAtPhase.get(phase));
            }

            // Call back with ContextData for advanced manipulation
            Consumer<Map<String,Object>> consumer = this.phaseContextDataConsumer.get(phase);

            if(consumer != null) consumer.accept(contextData);
        }

        @Override
        public String toString() {
            return String.format("UseCase-%d: %s", useCaseNumber, description);
        }

        public void addPhaseContextDataConsumer(InvocationPhase phase, Consumer<Map<String,Object>> consumer) {
            this.phaseContextDataConsumer.put(phase, consumer);
        }

        public void put(Map<String, Object> contextData, Object value) {
            contextData.put(contextDataUseCaseKey, value);
        }

        public void putData(InvocationPhase putDataAtPhase, Object value) {
            this.putDataAtPhase.put(putDataAtPhase, value);
        }

        public void putData(InvocationPhase putDataAtPhase) {
            putData(putDataAtPhase, getSimpleData());
        }

        public void removeData(InvocationPhase putDataAtPhase) {
            this.removeDataAtPhase.put(putDataAtPhase, this.contextDataUseCaseKey);
        }

        public String getSimpleData() {
            return String.format("%s-Data", contextDataUseCaseKey);
        }

        public String getSimpleData(String suffix) {
            return String.format("%s-%s", getSimpleData(), suffix);
        }

        public void addReturnKey(InvocationPhase addReturnKeyAtPhase) {
            this.addReturnKeyAtPhase.put(addReturnKeyAtPhase, this.contextDataUseCaseKey);
        }

        public void addExpectation(InvocationPhase addValueAtPhase) {
            addExpectation(addValueAtPhase, getSimpleData());
        }

        public void addExpectation(InvocationPhase addValueAtPhase, Object value) {
            this.addExpectationAtPhase.put(addValueAtPhase, value);
        }

        public void addExpectation(InvocationPhase addValueAtPhase, InvocationPhase removeExpectationAtPhase) {
            addExpectation(addValueAtPhase, removeExpectationAtPhase, getSimpleData());
        }

        public void addExpectation(InvocationPhase addValueAtPhase, InvocationPhase removeExpectationAtPhase, Object value) {
            addExpectation(addValueAtPhase, value);
            removeExpectation(removeExpectationAtPhase);
        }

        public void removeExpectation(InvocationPhase removeExpectationAtPhase) {
            this.removeExpectationAtPhase.add(removeExpectationAtPhase);
        }
    }
}
