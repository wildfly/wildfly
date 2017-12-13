/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.multinode.security.api;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author bmaxwell
 *
 */
@XmlRootElement(name="request-response")
@XmlAccessorType(XmlAccessType.FIELD)
public class EJBRequest implements Serializable {

    @XmlAttribute(name="caller")
    private String caller;

    @XmlElement(name="ejb-action")
    private List<EJBAction> actions = new ArrayList<>();

    @XmlElement(name="invocation-path")
    private List<InvocationPath> invocationPath = new ArrayList<>();

    public EJBRequest() {
    }

    public EJBRequest(String nodeName, String caller) {
        this.caller = caller;
        invocationPath.add(new InvocationPath(nodeName, caller));
    }

    public List<InvocationPath> getInvocationPath() {
        return invocationPath;
    }

    public void addAction(EJBAction action) {
        this.actions.add(action);
    }

    public String getCaller() {
        return caller;
    }

    public List<EJBAction> getActions() {
        return actions;
    }

    public void throwIfAnyExceptions() throws Throwable {
        for(InvocationPath path : invocationPath)
            if(path.getException() != null)
                throw path.getException();
    }

    public String getURLParams() {
        try {
            return String.format("%s=%s", "ejbRequest", URLEncoder.encode(this.marshall(), StandardCharsets.UTF_8.name()));
        } catch(UnsupportedEncodingException | JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public String marshall() throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(EJBRequest.class, EJBAction.class, InvocationPath.class, RemoteEJBConfig.class, EJBInfo.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ctx.createMarshaller().marshal(this, baos);
        return new String(baos.toByteArray());
    }

    public static EJBRequest unmarshall(String string) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(EJBRequest.class, EJBAction.class, InvocationPath.class, RemoteEJBConfig.class, EJBInfo.class);
        return (EJBRequest) ctx.createUnmarshaller().unmarshal(new StringReader(string));
    }

    public String getResponseInvocationPath() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Caller: %s\n", caller));
        sb.append(String.format("Invocation path:\n"));
        for(InvocationPath path : getInvocationPath())
            sb.append(String.format("  Node: %s callerPrincipal: %s\n", path.getNodeName(), path.getCallerPrincipal()));
        sb.append("-------------------------------------");
        return sb.toString();
    }
}