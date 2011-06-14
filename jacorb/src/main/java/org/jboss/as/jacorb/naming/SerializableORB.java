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

package org.jboss.as.jacorb.naming;

import org.omg.CORBA.Any;
import org.omg.CORBA.Context;
import org.omg.CORBA.ContextList;
import org.omg.CORBA.Current;
import org.omg.CORBA.Environment;
import org.omg.CORBA.ExceptionList;
import org.omg.CORBA.NVList;
import org.omg.CORBA.NamedValue;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.Object;
import org.omg.CORBA.OperationDef;
import org.omg.CORBA.Policy;
import org.omg.CORBA.PolicyError;
import org.omg.CORBA.Request;
import org.omg.CORBA.ServiceInformationHolder;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.UnionMember;
import org.omg.CORBA.ValueMember;
import org.omg.CORBA.WrongTransaction;
import org.omg.CORBA.portable.OutputStream;

import java.applet.Applet;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Properties;

/**
 * <p>
 * This class implements a serializable {@code ORB}. It provides the {@code readResolve} method that restores the ORB
 * when upon de-serialization.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@SuppressWarnings("unused")
public class SerializableORB extends ORB implements Serializable {

    private static final long serialVersionUID = -916585825830592748L;

    private transient ORB delegate;

    public SerializableORB(ORB delegate) {
        if (delegate == null)
            throw new IllegalArgumentException("Null delegate");
        this.delegate = delegate;
    }

    @Override
    public void connect(Object obj) {
        delegate.connect(obj);
    }

    @Override
    public TypeCode create_abstract_interface_tc(String id, String name) {
        return delegate.create_abstract_interface_tc(id, name);
    }

    @Override
    public TypeCode create_alias_tc(String id, String name, TypeCode original_type) {
        return delegate.create_alias_tc(id, name, original_type);
    }

    @Override
    public Any create_any() {
        return delegate.create_any();
    }

    @Override
    public TypeCode create_array_tc(int length, TypeCode element_type) {
        return delegate.create_array_tc(length, element_type);
    }

    @Override
    public ContextList create_context_list() {
        return delegate.create_context_list();
    }

    @Override
    public TypeCode create_enum_tc(String id, String name, String[] members) {
        return delegate.create_enum_tc(id, name, members);
    }

    @Override
    public Environment create_environment() {
        return delegate.create_environment();
    }

    @Override
    public ExceptionList create_exception_list() {
        return delegate.create_exception_list();
    }

    @Override
    public TypeCode create_exception_tc(String id, String name, StructMember[] members) {
        return delegate.create_exception_tc(id, name, members);
    }

    @Override
    public TypeCode create_fixed_tc(short digits, short scale) {
        return delegate.create_fixed_tc(digits, scale);
    }

    @Override
    public TypeCode create_interface_tc(String id, String name) {
        return delegate.create_interface_tc(id, name);
    }

    @Override
    public NVList create_list(int count) {
        return delegate.create_list(count);
    }

    @Override
    public NamedValue create_named_value(String s, Any any, int flags) {
        return delegate.create_named_value(s, any, flags);
    }

    @Override
    public TypeCode create_native_tc(String id, String name) {
        return delegate.create_native_tc(id, name);
    }

    @Override
    public NVList create_operation_list(Object oper) {
        return delegate.create_operation_list(oper);
    }

    //@Override Intellij keeps JDK in front
    public NVList create_operation_list(OperationDef operationDef) {
        return delegate.create_operation_list(operationDef);
    }

    @Override
    public OutputStream create_output_stream() {
        return delegate.create_output_stream();
    }

    @Override
    public Policy create_policy(int type, Any val) throws PolicyError {
        return delegate.create_policy(type, val);
    }

    @Override
    public TypeCode create_recursive_sequence_tc(int bound, int offset) {
        return delegate.create_recursive_sequence_tc(bound, offset);
    }

    @Override
    public TypeCode create_recursive_tc(String id) {
        return delegate.create_recursive_tc(id);
    }

    @Override
    public TypeCode create_sequence_tc(int bound, TypeCode element_type) {
        return delegate.create_sequence_tc(bound, element_type);
    }

    @Override
    public TypeCode create_string_tc(int bound) {
        return delegate.create_string_tc(bound);
    }

    @Override
    public TypeCode create_struct_tc(String id, String name, StructMember[] members) {
        return delegate.create_struct_tc(id, name, members);
    }

    @Override
    public TypeCode create_union_tc(String id, String name, TypeCode discriminator_type, UnionMember[] members) {
        return delegate.create_union_tc(id, name, discriminator_type, members);
    }

    @Override
    public TypeCode create_value_box_tc(String id, String name, TypeCode boxed_type) {
        return delegate.create_value_box_tc(id, name, boxed_type);
    }

    @Override
    public TypeCode create_value_tc(String id, String name, short type_modifier, TypeCode concrete_base,
                                    ValueMember[] members) {
        return delegate.create_value_tc(id, name, type_modifier, concrete_base, members);
    }

    @Override
    public TypeCode create_wstring_tc(int bound) {
        return delegate.create_wstring_tc(bound);
    }

    @Override
    public void destroy() {
        delegate.destroy();
    }

    @Override
    public void disconnect(Object obj) {
        delegate.disconnect(obj);
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        return (obj instanceof ORB) && delegate.equals(obj);
    }

    @Override
    public Current get_current() {
        return delegate.get_current();
    }

    @Override
    public Context get_default_context() {
        return delegate.get_default_context();
    }

    @Override
    public Request get_next_response() throws WrongTransaction {
        return delegate.get_next_response();
    }

    @Override
    public TypeCode get_primitive_tc(TCKind tcKind) {
        return delegate.get_primitive_tc(tcKind);
    }

    @Override
    public boolean get_service_information(short service_type, ServiceInformationHolder service_info) {
        return delegate.get_service_information(service_type, service_info);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String[] list_initial_services() {
        return delegate.list_initial_services();
    }

    @Override
    public String object_to_string(Object obj) {
        return delegate.object_to_string(obj);
    }

    @Override
    public void perform_work() {
        delegate.perform_work();
    }

    @Override
    public boolean poll_next_response() {
        return delegate.poll_next_response();
    }

    @Override
    public Object resolve_initial_references(String object_name) throws InvalidName {
        return delegate.resolve_initial_references(object_name);
    }

    @Override
    public void run() {
        delegate.run();
    }

    @Override
    public void send_multiple_requests_deferred(Request[] req) {
        delegate.send_multiple_requests_deferred(req);
    }

    @Override
    public void send_multiple_requests_oneway(Request[] req) {
        delegate.send_multiple_requests_oneway(req);
    }

    @Override
    public void shutdown(boolean wait_for_completion) {
        delegate.shutdown(wait_for_completion);
    }

    @Override
    public Object string_to_object(String str) {
        return delegate.string_to_object(str);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean work_pending() {
        return delegate.work_pending();
    }

    @Override
    protected void set_parameters(Applet app, Properties props) {
        throw new UnsupportedOperationException("set_parameters");
    }

    @Override
    protected void set_parameters(String[] args, Properties props) {
        throw new UnsupportedOperationException("set_parameters");
    }

    java.lang.Object readResolve() throws ObjectStreamException {
        return ORBInitialContextFactory.getORB();
    }
}
