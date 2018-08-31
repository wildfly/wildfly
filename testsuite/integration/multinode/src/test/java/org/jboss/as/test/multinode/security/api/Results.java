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
import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.junit.Assert;

/**
 * @author bmaxwell
 *
 */
@XmlRootElement(name="results")
@XmlAccessorType(XmlAccessType.FIELD)
public class Results {

    @XmlElement(name="webUser")
    private String webUser;

    @XmlElement(name="info")
    private String info;

    @XmlElement(name="response")
    private EJBRequest response;

    @XmlJavaTypeAdapter(ThrowableAdapter.class)
    @XmlElement(name="exception")
    private Throwable exception;

    public Results() {
    }

    public Results(Throwable exception) {
        this.exception = exception;
    }
    public Results(String webUser) {
        this.webUser = webUser;
    }
    public void setException(Throwable exception) {
        this.exception = exception;
    }
    public Results(String webUser, EJBRequest response) {
        this.webUser = webUser;
        this.response = response;
    }
    public EJBRequest getResponse() {
        return response;
    }

    public Throwable getException() {
        return exception;
    }
    public String getInfo() {
        return info;
    }
    public void setInfo(String info) {
        this.info = info;
    }
    public void addInfo(String info) {
        this.info += " " + info;
    }
    public void add(EJBRequest response) {
        this.response = response;
    }
    public String getWebUser() {
        return webUser;
    }

    public void throwIfException() throws Exception {
        Assert.assertNull("Exception was thrown", exception);
        if(exception != null)
            throw new Exception(exception);
        for(InvocationPath path : response.getInvocationPath()) {
            if(path.getException() != null)
                throw new Exception(path.getException());
        }
    }

    public void failIfCallerIsNot(int invocationPathIndex, String username) throws Exception {
        Assert.assertEquals("caller is wrong", username, response.getInvocationPath().get(invocationPathIndex).getCallerPrincipal());
    }

    public void failIfEJBNodeNameIsNot(int invocationPathIndex, String nodeName) throws Exception {
        Assert.assertEquals("jboss.node.name is wrong", nodeName, response.getInvocationPath().get(invocationPathIndex).getNodeName());
    }

    public String marshall() throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(Results.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ctx.createMarshaller().marshal(this, baos);
        return new String(baos.toByteArray());
    }

    public static Results unmarshall(String string) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(Results.class);
        Results results = (Results) ctx.createUnmarshaller().unmarshal(new StringReader(string));
        results.throwIfException();
        return results;
    }
}