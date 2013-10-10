/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.spec.api.test;

import javax.annotation.*;
import javax.annotation.security.*;
import javax.annotation.sql.*;
import javax.batch.api.*;
import javax.batch.api.chunk.*;
import javax.batch.api.chunk.listener.*;
import javax.batch.api.listener.*;
import javax.batch.api.partition.*;
import javax.batch.operations.*;
import javax.batch.runtime.*;
import javax.batch.runtime.context.*;
import javax.decorator.*;
import javax.ejb.*;
import javax.ejb.embeddable.*;
import javax.ejb.spi.*;
import javax.el.*;
import javax.enterprise.concurrent.*;
import javax.enterprise.context.*;
import javax.enterprise.context.spi.*;
import javax.enterprise.event.*;
import javax.enterprise.inject.*;
import javax.enterprise.inject.spi.*;
import javax.enterprise.util.*;
import javax.faces.*;
import javax.faces.application.*;
import javax.faces.bean.*;
import javax.faces.component.*;
import javax.faces.component.behavior.*;
import javax.faces.component.html.*;
import javax.faces.component.visit.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.flow.*;
import javax.faces.lifecycle.*;
import javax.faces.model.*;
import javax.faces.render.*;
import javax.faces.validator.*;
import javax.faces.view.*;
import javax.faces.view.facelets.*;
import javax.faces.webapp.*;
import javax.inject.*;
import javax.interceptor.*;
import javax.jms.*;
import javax.json.*;
import javax.json.spi.*;
import javax.json.stream.*;
import javax.jws.*;
import javax.jws.soap.*;
import javax.mail.*;
import javax.mail.event.*;
import javax.mail.internet.*;
import javax.mail.search.*;
import javax.mail.util.*;
import javax.management.j2ee.*;
import javax.management.j2ee.statistics.*;
import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.*;
import javax.persistence.spi.*;
import javax.resource.*;
import javax.resource.cci.*;
import javax.resource.spi.*;
import javax.resource.spi.endpoint.*;
import javax.resource.spi.security.*;
import javax.resource.spi.work.*;
import javax.security.auth.message.*;
import javax.security.auth.message.callback.*;
import javax.security.auth.message.config.*;
import javax.security.auth.message.module.*;
import javax.security.jacc.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.descriptor.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.el.*;
import javax.servlet.jsp.jstl.core.*;
import javax.servlet.jsp.jstl.fmt.*;
import javax.servlet.jsp.jstl.sql.*;
import javax.servlet.jsp.jstl.tlv.*;
import javax.servlet.jsp.tagext.*;
import javax.transaction.*;
import javax.transaction.xa.*;
import javax.validation.*;
import javax.validation.bootstrap.*;
import javax.validation.constraints.*;
import javax.validation.constraintvalidation.*;
import javax.validation.executable.*;
import javax.validation.groups.*;
import javax.validation.metadata.*;
import javax.validation.spi.*;
import javax.websocket.*;
import javax.websocket.server.*;
import javax.ws.rs.*;
import javax.ws.rs.client.*;
import javax.ws.rs.container.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import javax.xml.bind.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.*;
import javax.xml.bind.attachment.*;
import javax.xml.bind.helpers.*;
import javax.xml.bind.util.*;
import javax.xml.rpc.*;
import javax.xml.rpc.encoding.*;
import javax.xml.rpc.handler.*;
import javax.xml.rpc.handler.soap.*;
import javax.xml.rpc.holders.*;
import javax.xml.rpc.server.*;
import javax.xml.rpc.soap.*;
import javax.xml.soap.*;
import javax.xml.ws.*;
import javax.xml.ws.handler.*;
import javax.xml.ws.handler.soap.*;
import javax.xml.ws.http.*;
import javax.xml.ws.soap.*;
import javax.xml.ws.spi.*;
import javax.xml.ws.spi.http.*;
import javax.xml.ws.wsaddressing.*;

/**
 * This class checks the dependencies as exported by the
 * "spec-api" module to ensure that compilation of all Java EE
 * 7 Specification Platform APIs are reachable.  As such, no runtime
 * assertions are required here, only references.  If this class compiles, 
 * all noted references are reachable within the spec-api dependency chain. 
 * 
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 * @see http://docs.oracle.com/javaee/7/api/
 */
@SuppressWarnings("unused")
public class SpecApiDependencyCheck {

    
    
}
