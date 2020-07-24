/*
 * Copyright 2020 Red Hat, Inc.
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

package org.wildfly.test.security.common.elytron;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;

/**
 * A {@link ConfigurableElement} to define the ldap-realm resource within the Elytron subsystem.
 *
 * @author Ondrej Kotek
 */
public class LdapRealm implements SecurityRealm {

    private final PathAddress address;
    private final String name;
    private final String dirContext;
    private final Boolean directVerification;
    private final Boolean allowBlankPassword;
    private final IdentityMapping identityMapping;

    LdapRealm(Builder builder) {
        this.name = builder.name;
        this.address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "elytron"), PathElement.pathElement("ldap-realm", name));
        this.dirContext = builder.dirContext;
        this.directVerification = builder.directVerification;
        this.allowBlankPassword = builder.allowBlankPassword;
        this.identityMapping = builder.identityMapping;
    }

    @Override
    public String getName() {
        return name;
    }

    public ModelNode getAddOperation() {
        ModelNode addOperation = Util.createAddOperation(address);
        addOperation.get("ldap-realm");
        if (dirContext != null) {
            addOperation.get("dir-context").set(dirContext);
        }
        if (directVerification != null) {
            addOperation.get("direct-verification").set(directVerification);
        }
        if (allowBlankPassword != null) {
            addOperation.get("allow-blank-password").set(allowBlankPassword);
        }
        if (identityMapping != null) {
            addOperation.get("identity-mapping").set(getIdentityMappingModel().asObject());
        }

        return addOperation;
    }

    private ModelNode getIdentityMappingModel() {
        ModelNode identityMappingModelNode = new ModelNode();
        if (!identityMapping.getAttributeMappings().isEmpty()) {
            List<ModelNode> attributeMappingNodeList = new ArrayList<ModelNode>();
            for (AttributeMapping attributeMapping : identityMapping.getAttributeMappings()) {
                ModelNode node = new ModelNode();
                if (attributeMapping.getFrom() != null) {
                    node.add("from", attributeMapping.getFrom());
                }
                if (attributeMapping.getTo() != null) {
                    node.add("to", attributeMapping.getTo());
                }
                if (attributeMapping.getFilter() != null) {
                    node.add("filter", attributeMapping.getFilter());
                }
                if (attributeMapping.getFilterBaseDn() != null) {
                    node.add("filter-base-dn", attributeMapping.getFilterBaseDn());
                }
                if (attributeMapping.getExtractRdn() != null) {
                    node.add("extract-rdn", attributeMapping.getExtractRdn());
                }
                if (attributeMapping.getSearchRecursive() != null) {
                    node.add("search-recursive", attributeMapping.getSearchRecursive());
                }
                if (attributeMapping.getRoleRecursion() != null) {
                    node.add("role-recursion", attributeMapping.getRoleRecursion());
                }
                if (attributeMapping.getRoleRecursionName() != null) {
                    node.add("role-recursion-name", attributeMapping.getRoleRecursionName());
                }
                if (attributeMapping.getReference() != null) {
                    node.add("reference", attributeMapping.getReference());
                }
                attributeMappingNodeList.add(node.asObject());
            }
            ModelNode attributeMappingNode = new ModelNode();
            attributeMappingNode.set(attributeMappingNodeList);
            identityMappingModelNode.add("attribute-mapping", attributeMappingNode);
        }
        if (identityMapping.getFilterName() != null) {
            identityMappingModelNode.add("filter-name", identityMapping.getFilterName());
        }
        if (identityMapping.getIteratorFilter() != null) {
            identityMappingModelNode.add("iterator-filter", identityMapping.getIteratorFilter());
        }
        if (!identityMapping.getNewIdentityAttributes().isEmpty()) {
            List<ModelNode> newIdentityAttributesNodeList = new ArrayList<ModelNode>();
            for (NewIdentityAttributes newIdentityAttribute : identityMapping.getNewIdentityAttributes()) {
                ModelNode attributeNode = new ModelNode();
                if (newIdentityAttribute.getName() != null) {
                    attributeNode.add("name", newIdentityAttribute.getName());
                }

                ModelNode valuesList = new ModelNode().setEmptyList();
                for (String value : newIdentityAttribute.getValues()) {
                    valuesList.add(value);
                }
                attributeNode.add("value", valuesList);

                newIdentityAttributesNodeList.add(attributeNode.asObject());
            }
            ModelNode newIdentityAttributesNode = new ModelNode();
            newIdentityAttributesNode.set(newIdentityAttributesNodeList);
            identityMappingModelNode.add("new-identity-attributes", newIdentityAttributesNode);
        }
        if (identityMapping.getNewIdentityParentDn() != null) {
            identityMappingModelNode.add("new-identity-parent-dn", identityMapping.getNewIdentityParentDn());
        }
        if (identityMapping.getOtpCredentialMapper() != null) {
            OtpCredentialMapper otpCredentialMapper = identityMapping.getOtpCredentialMapper();
            ModelNode node = new ModelNode();
            node.add("algorithm-from", otpCredentialMapper.getAlgorithmFrom());
            node.add("hash-from", otpCredentialMapper.getHashFrom());
            node.add("seed-from", otpCredentialMapper.getSeedFrom());
            node.add("sequence-from", otpCredentialMapper.getSequenceFrom());
            identityMappingModelNode.add("otp-credential-mapper", node.asObject());
        }
        if (identityMapping.getRdnIdentifier() != null) {
            identityMappingModelNode.add("rdn-identifier", identityMapping.getRdnIdentifier());
        }
        if (identityMapping.getSearchBaseDn() != null) {
            identityMappingModelNode.add("search-base-dn", identityMapping.getSearchBaseDn());
        }
        if (identityMapping.getUseRecursiveSearch() != null) {
            identityMappingModelNode.add("use-recursive-search", identityMapping.getUseRecursiveSearch());
        }
        if (identityMapping.getUserPasswordMapper() != null) {
            UserPasswordMapper userPasswordMapper = identityMapping.getUserPasswordMapper();
            ModelNode node = new ModelNode();
            node.add("from", userPasswordMapper.getFrom());
            if (userPasswordMapper.getWritable() != null) {
                node.add("writable", userPasswordMapper.getWritable());
            }
            if (userPasswordMapper.getVerifiable() != null) {
                node.add("verifiable", userPasswordMapper.getVerifiable());
            }
            identityMappingModelNode.add("user-password-mapper", node.asObject());
        }
        if (identityMapping.getX509CredentialMapper() != null) {
            X509CredentialMapper x509CredentialMapper = identityMapping.getX509CredentialMapper();
            ModelNode node = new ModelNode();
            if (x509CredentialMapper.getDigestFrom() != null) {
                node.add("digest-from", x509CredentialMapper.getDigestFrom());
            }
            if (x509CredentialMapper.getDigestAlgorithm() != null) {
                node.add("digest-algorithm", x509CredentialMapper.getDigestAlgorithm());
            }
            if (x509CredentialMapper.getCertificateFrom() != null) {
                node.add("certificate-from", x509CredentialMapper.getCertificateFrom());
            }
            if (x509CredentialMapper.getSerialNumberFrom() != null) {
                node.add("serial-number-from", x509CredentialMapper.getSerialNumberFrom());
            }
            if (x509CredentialMapper.getSubjectDnFrom() != null) {
                node.add("subject-dn-from", x509CredentialMapper.getSubjectDnFrom());
            }
            identityMappingModelNode.add("x509-credential-mapper", node.asObject());
        }
        return identityMappingModelNode;
    }

    public ModelNode getRemoveOperation() {
        return Util.createRemoveOperation(address);
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(getAddOperation(), client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(getRemoveOperation(), client);
    }

    public static Builder builder(final String name) {
        return new Builder(name);
    }

    public static IdentityMappingBuilder identityMappingBuilder() {
        return new IdentityMappingBuilder();
    }

    public static final class Builder {

        private final String name;
        private String dirContext;
        private Boolean directVerification;
        private Boolean allowBlankPassword;
        private IdentityMapping identityMapping;

        public Builder(String name) {
            this.name = name;
        }

        public Builder withDirContext(String dirContext) {
            this.dirContext = dirContext;

            return this;
        }

        public Builder withDirectVerification(Boolean directVerification) {
            this.directVerification = directVerification;

            return this;
        }

        public Builder withAllowBlankPassword(Boolean allowBlankPassword) {
            this.allowBlankPassword = allowBlankPassword;

            return this;
        }

        public Builder withIdentityMapping(IdentityMapping identityMapping) {
            this.identityMapping = identityMapping;

            return this;
        }

        public LdapRealm build() {
            return new LdapRealm(this);
        }
    }

    public static final class IdentityMapping {

        private final String rdnIdentifier;
        private final String searchBaseDn;
        private final Boolean useRecursiveSearch;
        private final String filterName;
        private final String iteratorFilter;
        private final String newIdentityParentDn;
        private final List<AttributeMapping> attributeMappings;
        private final UserPasswordMapper userPasswordMapper;
        private final OtpCredentialMapper otpCredentialMapper;
        private final X509CredentialMapper x509CredentialMapper;
        private final List<NewIdentityAttributes> newIdentityAttributes;

        private IdentityMapping(IdentityMappingBuilder builder) {
            this.rdnIdentifier = builder.rdnIdentifier;
            this.searchBaseDn = builder.searchBaseDn;
            this.useRecursiveSearch = builder.useRecursiveSearch;
            this.filterName = builder.filterName;
            this.iteratorFilter = builder.iteratorFilter;
            this.newIdentityParentDn = builder.newIdentityParentDn;
            this.attributeMappings = builder.attributeMappings;
            this.userPasswordMapper = builder.userPasswordMapper;
            this.otpCredentialMapper = builder.otpCredentialMapper;
            this.newIdentityAttributes = builder.newIdentityAttributes;
            this.x509CredentialMapper = builder.x509CredentialMapper;
        }

        public String getRdnIdentifier() {
            return rdnIdentifier;
        }

        public String getSearchBaseDn() {
            return searchBaseDn;
        }

        public Boolean getUseRecursiveSearch() {
            return useRecursiveSearch;
        }

        public String getFilterName() {
            return filterName;
        }

        public String getIteratorFilter() {
            return iteratorFilter;
        }

        public String getNewIdentityParentDn() {
            return newIdentityParentDn;
        }

        public List<AttributeMapping> getAttributeMappings() {
            return attributeMappings;
        }

        public UserPasswordMapper getUserPasswordMapper() {
            return userPasswordMapper;
        }

        public OtpCredentialMapper getOtpCredentialMapper() {
            return otpCredentialMapper;
        }

        public X509CredentialMapper getX509CredentialMapper() {
            return x509CredentialMapper;
        }

        public List<NewIdentityAttributes> getNewIdentityAttributes() {
            return newIdentityAttributes;
        }

    }

    public static final class IdentityMappingBuilder {

        private String rdnIdentifier;
        private String searchBaseDn;
        private Boolean useRecursiveSearch;
        private String filterName;
        private String iteratorFilter;
        private String newIdentityParentDn;
        private List<AttributeMapping> attributeMappings = new ArrayList<AttributeMapping>();
        private UserPasswordMapper userPasswordMapper;
        private OtpCredentialMapper otpCredentialMapper;
        private X509CredentialMapper x509CredentialMapper;
        private List<NewIdentityAttributes> newIdentityAttributes = new ArrayList<NewIdentityAttributes>();

        public IdentityMappingBuilder withRdnIdentifier(String rdnIdentifier) {
            this.rdnIdentifier = rdnIdentifier;

            return this;
        }

        public IdentityMappingBuilder withSearchBaseDn(String searchBaseDn) {
            this.searchBaseDn = searchBaseDn;

            return this;
        }

        public IdentityMappingBuilder withUseRecursiveSearch(Boolean useRecursiveSearch) {
            this.useRecursiveSearch = useRecursiveSearch;

            return this;
        }

        public IdentityMappingBuilder withFilterName(String filterName) {
            this.filterName = filterName;

            return this;
        }

        public IdentityMappingBuilder withIteratorFilter(String iteratorFilter) {
            this.iteratorFilter = iteratorFilter;

            return this;
        }

        public IdentityMappingBuilder withNewIdentityParentDn(String newIdentityParentDn) {
            this.newIdentityParentDn = newIdentityParentDn;

            return this;
        }

        public IdentityMappingBuilder withAttributeMappings(AttributeMapping... attributeMappings) {
            if (attributeMappings != null) {
                Collections.addAll(this.attributeMappings, attributeMappings);
            }

            return this;
        }

        public IdentityMappingBuilder withUserPasswordMapper(UserPasswordMapper userPasswordMapper) {
            this.userPasswordMapper = userPasswordMapper;

            return this;
        }

        public IdentityMappingBuilder withOtpCredentialMapper(OtpCredentialMapper otpCredentialMapper) {
            this.otpCredentialMapper = otpCredentialMapper;

            return this;
        }

        public IdentityMappingBuilder withX509CredentialMapper(X509CredentialMapper x509CredentialMapper) {
            this.x509CredentialMapper = x509CredentialMapper;

            return this;
        }

        public IdentityMappingBuilder withNewIdentityAttributes(NewIdentityAttributes... newIdentityAttributes) {
            if (newIdentityAttributes != null) {
                Collections.addAll(this.newIdentityAttributes, newIdentityAttributes);
            }

            return this;
        }

        public IdentityMapping build() {
            return new IdentityMapping(this);
        }
    }

    public static final class AttributeMapping {

        private final String from;
        private final String to;
        private final String filter;
        private final String filterBaseDn;
        private final String extractRdn;
        private final Boolean searchRecursive;
        private final Integer roleRecursion;
        private final String roleRecursionName;
        private final String reference;

        private AttributeMapping(AttributeMappingBuilder builder) {
            this.from = builder.from;
            this.to = builder.to;
            this.filter = builder.filter;
            this.filterBaseDn = builder.filterBaseDn;
            this.extractRdn = builder.extractRdn;
            this.searchRecursive = builder.searchRecursive;
            this.roleRecursion = builder.roleRecursion;
            this.roleRecursionName = builder.roleRecursionName;
            this.reference = builder.reference;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public String getFilter() {
            return filter;
        }

        public String getFilterBaseDn() {
            return filterBaseDn;
        }

        public String getExtractRdn() {
            return extractRdn;
        }

        public Boolean getSearchRecursive() {
            return searchRecursive;
        }

        public Integer getRoleRecursion() {
            return roleRecursion;
        }

        public String getRoleRecursionName() {
            return roleRecursionName;
        }

        public String getReference() {
            return reference;
        }

    }

    public static final class AttributeMappingBuilder {

        private String from;
        private String to;
        private String filter;
        private String filterBaseDn;
        private String extractRdn;
        private Boolean searchRecursive;
        private Integer roleRecursion;
        private String roleRecursionName;
        private String reference;

        public AttributeMappingBuilder withFrom(String from) {
            this.from = from;

            return this;
        }

        public AttributeMappingBuilder withTo(String to) {
            this.to = to;

            return this;
        }

        public AttributeMappingBuilder withFilter(String filter) {
            this.filter = filter;

            return this;
        }

        public AttributeMappingBuilder withFilterBaseDn(String filterBaseDn) {
            this.filterBaseDn = filterBaseDn;

            return this;
        }

        public AttributeMappingBuilder withExtractRdn(String extractRdn) {
            this.extractRdn = extractRdn;

            return this;
        }

        public AttributeMappingBuilder withSearchRecursive(Boolean searchRecursive) {
            this.searchRecursive = searchRecursive;

            return this;
        }

        public AttributeMappingBuilder withRoleRecursion(Integer roleRecursion) {
            this.roleRecursion = roleRecursion;

            return this;
        }

        public AttributeMappingBuilder withRoleRecursionName(String roleRecursionName) {
            this.roleRecursionName = roleRecursionName;

            return this;
        }

        public AttributeMappingBuilder withReference(String reference) {
            this.reference = reference;

            return this;
        }

        public AttributeMapping build() {
            return new AttributeMapping(this);
        }

    }

    public static final class UserPasswordMapper {

        private final String from;
        private final Boolean writable;
        private final Boolean verifiable;

        private UserPasswordMapper(UserPasswordMapperBuilder builder) {
            this.from = builder.from;
            this.writable = builder.writable;
            this.verifiable = builder.verifiable;
        }

        public String getFrom() {
            return from;
        }

        public Boolean getWritable() {
            return writable;
        }

        public Boolean getVerifiable() {
            return verifiable;
        }

    }

    public static final class UserPasswordMapperBuilder {

        private String from;
        private Boolean writable;
        private Boolean verifiable;

        public UserPasswordMapperBuilder withFrom(String from) {
            this.from = from;

            return this;
        }

        public UserPasswordMapperBuilder withWritable(Boolean writable) {
            this.writable = writable;

            return this;
        }

        public UserPasswordMapperBuilder withVerifiable(Boolean verifiable) {
            this.verifiable = verifiable;

            return this;
        }

        public UserPasswordMapper build() {
            return new UserPasswordMapper(this);
        }
    }

    public static final class OtpCredentialMapper {

        private final String algorithmFrom;
        private final String hashFrom;
        private final String seedFrom;
        private final String sequenceFrom;

        private OtpCredentialMapper(OtpCredentialMapperBuilder builder) {
            this.algorithmFrom = builder.algorithmFrom;
            this.hashFrom = builder.hashFrom;
            this.seedFrom = builder.seedFrom;
            this.sequenceFrom = builder.sequenceFrom;
        }

        public String getAlgorithmFrom() {
            return algorithmFrom;
        }

        public String getHashFrom() {
            return hashFrom;
        }

        public String getSeedFrom() {
            return seedFrom;
        }

        public String getSequenceFrom() {
            return sequenceFrom;
        }

    }

    public static final class OtpCredentialMapperBuilder {

        private String algorithmFrom;
        private String hashFrom;
        private String seedFrom;
        private String sequenceFrom;

        public OtpCredentialMapperBuilder withAlgorithmFrom(String algorithmFrom) {
            this.algorithmFrom = algorithmFrom;

            return this;
        }

        public OtpCredentialMapperBuilder withHashFrom(String hashFrom) {
            this.hashFrom = hashFrom;

            return this;
        }

        public OtpCredentialMapperBuilder withSeedFrom(String seedFrom) {
            this.seedFrom = seedFrom;
            return this;
        }

        public OtpCredentialMapperBuilder withSequenceFrom(String sequenceFrom) {
            this.sequenceFrom = sequenceFrom;

            return this;
        }

        public OtpCredentialMapper build() {
            return new OtpCredentialMapper(this);
        }
    }

    public static final class X509CredentialMapper {

        private final String digestFrom;
        private final String digestAlgorithm;
        private final String certificateFrom;
        private final String serialNumberFrom;
        private final String subjectDnFrom;

        private X509CredentialMapper(X509CredentialMapperBuilder builder) {
            this.digestFrom = builder.digestFrom;
            this.digestAlgorithm = builder.digestAlgorithm;
            this.certificateFrom = builder.certificateFrom;
            this.serialNumberFrom = builder.serialNumberFrom;
            this.subjectDnFrom = builder.subjectDnFrom;
        }

        public String getDigestFrom() {
            return digestFrom;
        }

        public String getDigestAlgorithm() {
            return digestAlgorithm;
        }

        public String getCertificateFrom() {
            return certificateFrom;
        }

        public String getSerialNumberFrom() {
            return serialNumberFrom;
        }

        public String getSubjectDnFrom() {
            return subjectDnFrom;
        }

    }

    public static final class X509CredentialMapperBuilder {

        private String digestFrom;
        private String digestAlgorithm;
        private String certificateFrom;
        private String serialNumberFrom;
        private String subjectDnFrom;

        public X509CredentialMapperBuilder withDigestFrom(String digestFrom) {
            this.digestFrom = digestFrom;

            return this;
        }

        public X509CredentialMapperBuilder withDigestAlgorithm(String digestAlgorithm) {
            this.digestAlgorithm = digestAlgorithm;

            return this;
        }

        public X509CredentialMapperBuilder withCertificateFrom(String certificateFrom) {
            this.certificateFrom = certificateFrom;

            return this;
        }

        public X509CredentialMapperBuilder withSerialNumberFrom(String serialNumberFrom) {
            this.serialNumberFrom = serialNumberFrom;

            return this;
        }

        public X509CredentialMapperBuilder withSubjectDnFrom(String subjectDnFrom) {
            this.subjectDnFrom = subjectDnFrom;

            return this;
        }

        public X509CredentialMapper build() {
            return new X509CredentialMapper(this);
        }
    }

    public static final class NewIdentityAttributes {

        private final String name;
        private List<String> values;

        private NewIdentityAttributes(NewIdentityAttributesBuilder builder) {
            this.name = builder.name;
            this.values = builder.values;
        }

        public String getName() {
            return name;
        }

        public List<String> getValues() {
            return values;
        }

    }

    public static final class NewIdentityAttributesBuilder {

        private String name;
        private List<String> values = new ArrayList<String>();

        public NewIdentityAttributesBuilder withName(String name) {
            this.name = name;

            return this;
        }

        public NewIdentityAttributesBuilder withValues(String... values) {
            if (values != null) {
                Collections.addAll(this.values, values);
            }

            return this;
        }

        public NewIdentityAttributes build() {
            return new NewIdentityAttributes(this);
        }
    }
}
