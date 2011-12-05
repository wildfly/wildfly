package org.jboss.as.test.integration.ejb.entity.cmp.postcreate;

import java.util.Collection;
import javax.ejb.*;
import java.rmi.*;

public interface Bean extends EJBObject {
    // Business Methods for Bean CMP Fields
    public String getId() throws RemoteException;

    public String getName() throws RemoteException;

    public void setName(String v) throws RemoteException;

    public int getValue() throws RemoteException;

    public void setValue(int v) throws RemoteException;

    public boolean test0() throws RemoteException;

    public Collection getAInfoFromB() throws RemoteException;

    public Collection getBInfoFromA() throws RemoteException;

    public boolean doAssignmentTest1() throws RemoteException;

    public boolean doAssignmentTest2() throws RemoteException;

    public boolean doAssignmentTest3() throws RemoteException;

    public boolean doAssignmentTest4() throws RemoteException;

    public boolean setCmrFieldToNull() throws RemoteException;

    public boolean setCmrFieldToWrongType(int i) throws RemoteException;
}
