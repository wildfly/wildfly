/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb3.stateful.passivation;

import javax.ejb.Stateful;

import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.ejb3.annotation.Clustered;
import org.jboss.logging.Logger;

/**
 * @author Ondrej Chaloupka
 */
@Clustered
@Stateful
public class StatefulBeanChild extends StatefulBeanParent implements StatefulBeanChildRemote {
    private static final Logger log = Logger.getLogger(StatefulBeanChild.class);
    
    private TestingDTO dto = new TestingDTO();
    private transient TestingDTO transientDto = new TestingDTO();
       
    public int getInt() {
        return intNumber;
    }
    public void setInt(int parentInt) {
        this.intNumber = parentInt;
    }
    
    public String getNodeName() {
        String nodeName = NodeNameGetter.getNodeName();
        log.info(this.getClass().getSimpleName() + " called on node " + nodeName);
        return nodeName;
    }
    
    // ---- Methods for manipulation with dto data ----
    public void setDTOStringData(String str) {
        dto.setData(str);
    }
    
    public String getDTOStringData() {
        return dto.getData();
    }
    
    public void setDTONumberData(int number) {
        dto.setNumber(number);
    }
    
    public int getDTONumberData() {
        return dto.getNumber();
    }
    
    public void setTransientDTOStringData(String str) {
        transientDto.setData(str);
    }
    
    public String getTransientDTOStringData() {
        return transientDto.getData();
    }
    
    public void setTransientDTONumberData(int number) {
        transientDto.setNumber(number);
    }
    
    public int getTransientDTONumberData() {
        return transientDto.getNumber();
    }
}
