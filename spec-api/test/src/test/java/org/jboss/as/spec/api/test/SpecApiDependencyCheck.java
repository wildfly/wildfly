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
