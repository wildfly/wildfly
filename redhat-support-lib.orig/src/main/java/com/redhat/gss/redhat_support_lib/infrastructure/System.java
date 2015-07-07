package com.redhat.gss.redhat_support_lib.infrastructure;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Complex type for System
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "system", propOrder = { "metadata", "machine_id",
        "display_name", "remote_branch", "remote_leaf", "account_number",
        "hostname", "last_check_in", "created_at", "updated_at",
        "unregistered_at", "is_blacklisted" })
public class System implements Serializable {

    private static final long serialVersionUID = 11L;
    @XmlElement(required = false)
    protected Map<String, String> metadata;
    @XmlElement(required = true)
    protected String machine_id;
    @XmlElement(required = false)
    protected String display_name;
    @XmlElement(required = false)
    protected String remote_branch;
    @XmlElement(required = false)
    protected String remote_leaf;
    @XmlElement(required = false)
    protected String account_number;
    @XmlElement(required = false)
    protected String hostname;
    @XmlElement(required = false)
    protected String last_check_in;
    @XmlElement(required = false)
    protected Date created_at;
    @XmlElement(required = false)
    protected Date updated_at;
    @XmlElement(required = false)
    protected Date unregistered_at;
    @XmlElement(required = false)
    protected boolean is_blacklisted;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getMachine_id() {
        return machine_id;
    }

    public void setMachine_id(String machine_id) {
        this.machine_id = machine_id;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public String getRemote_branch() {
        return remote_branch;
    }

    public void setRemote_branch(String remote_branch) {
        this.remote_branch = remote_branch;
    }

    public String getRemote_leaf() {
        return remote_leaf;
    }

    public void setRemote_leaf(String remote_leaf) {
        this.remote_leaf = remote_leaf;
    }

    public String getAccount_number() {
        return account_number;
    }

    public void setAccount_number(String account_number) {
        this.account_number = account_number;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getLast_check_in() {
        return last_check_in;
    }

    public void setLast_check_in(String last_check_in) {
        this.last_check_in = last_check_in;
    }

    public Date getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }

    public Date getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(Date updated_at) {
        this.updated_at = updated_at;
    }

    public Date getUnregistered_at() {
        return unregistered_at;
    }

    public void setUnregistered_at(Date unregistered_at) {
        this.unregistered_at = unregistered_at;
    }

    public boolean isIs_blacklisted() {
        return is_blacklisted;
    }

    public void setIs_blacklisted(boolean is_blacklisted) {
        this.is_blacklisted = is_blacklisted;
    }
}