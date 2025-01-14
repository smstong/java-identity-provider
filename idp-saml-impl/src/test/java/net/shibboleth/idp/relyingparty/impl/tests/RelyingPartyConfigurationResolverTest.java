/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.relyingparty.impl.tests;

import java.util.Iterator;
import java.util.List;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.profile.logic.EntityAttributesPredicate;
import org.opensaml.saml.common.profile.logic.EntityAttributesPredicate.Candidate;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.metadata.EntityGroupName;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.profile.relyingparty.BasicRelyingPartyConfiguration;
import net.shibboleth.profile.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.profile.relyingparty.RelyingPartyConfigurationResolver;
import net.shibboleth.profile.relyingparty.VerifiedProfileCriterion;
import net.shibboleth.profile.relyingparty.impl.DefaultRelyingPartyConfigurationResolver;
import net.shibboleth.saml.relyingparty.RelyingPartyConfigurationSupport;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;

/** Advanced unit tests for {@link RelyingPartyConfigurationResolver}. */
@SuppressWarnings("javadoc")
public class RelyingPartyConfigurationResolverTest extends XMLObjectBaseTestCase {
    
    private BasicRelyingPartyConfiguration anonRP, defaultRP; 
    
    private BasicRelyingPartyConfiguration oneByName, twoByName, threeByName;
    private BasicRelyingPartyConfiguration oneByGroup, twoByGroup;
    private BasicRelyingPartyConfiguration oneByTag, twoByTag;
    
    private DefaultRelyingPartyConfigurationResolver resolver;
        
    @BeforeMethod
    public void setup() throws Exception {
        anonRP = new BasicRelyingPartyConfiguration();
        anonRP.setId("anonRPId");
        anonRP.setIssuer("anonRPResp");
        anonRP.setDetailedErrors(true);
        anonRP.initialize();
        
        defaultRP = new BasicRelyingPartyConfiguration();
        defaultRP.setId("defaultRPId");
        defaultRP.setIssuer("defaultRPResp");
        defaultRP.setDetailedErrors(true);
        defaultRP.initialize();
        
        oneByName = RelyingPartyConfigurationSupport.byName(CollectionSupport.singleton("rp1"));
        oneByName.setIssuer("foo");
        oneByName.setDetailedErrors(true);
        oneByName.initialize();

        twoByName = RelyingPartyConfigurationSupport.byName(CollectionSupport.singleton("rp2"));
        twoByName.setIssuer("foo");
        twoByName.setDetailedErrors(true);
        twoByName.initialize();

        threeByName = RelyingPartyConfigurationSupport.byName(CollectionSupport.singleton("rp3"));
        threeByName.setIssuer("foo");
        threeByName.setDetailedErrors(true);
        threeByName.initialize();
        
        oneByGroup = RelyingPartyConfigurationSupport.byGroup(CollectionSupport.singleton("group1"), null);
        oneByGroup.setIssuer("foo");
        oneByGroup.setDetailedErrors(true);
        oneByGroup.initialize();
        
        twoByGroup = RelyingPartyConfigurationSupport.byGroup(CollectionSupport.singleton("group2"), null);
        twoByGroup.setIssuer("foo");
        twoByGroup.setDetailedErrors(true);
        twoByGroup.initialize();
        
        Candidate candidate1 = new EntityAttributesPredicate.Candidate("urn:test:attr:tag", Attribute.URI_REFERENCE);
        candidate1.setValues(CollectionSupport.singleton("tag1"));
        oneByTag = RelyingPartyConfigurationSupport.byTag(CollectionSupport.singleton(candidate1), true, true);
        oneByTag.setId("byTag1");
        oneByTag.setIssuer("foo");
        oneByTag.setDetailedErrors(true);
        oneByTag.initialize();
        
        Candidate candidate2 = new EntityAttributesPredicate.Candidate("urn:test:attr:tag", Attribute.URI_REFERENCE);
        candidate2.setValues(CollectionSupport.singleton("tag2"));
        twoByTag = RelyingPartyConfigurationSupport.byTag(CollectionSupport.singleton(candidate2), true, true);
        twoByTag.setId("byTag2");
        twoByTag.setIssuer("foo");
        twoByTag.setDetailedErrors(true);
        twoByTag.initialize();
        
        resolver = new DefaultRelyingPartyConfigurationResolver();
        resolver.setId("resolver");
        resolver.setUnverifiedConfiguration(anonRP);
        assert defaultRP!=null;
        resolver.setDefaultConfiguration(defaultRP);
    }
    
    @Test
    public void testResolveByEntityIDViaEntityIDCriterion() throws ComponentInitializationException, ResolverException {
        Iterable<RelyingPartyConfiguration> results = null;
        RelyingPartyConfiguration result = null;
        
        final List<RelyingPartyConfiguration> rpConfigs = CollectionSupport.listOf(oneByName, twoByName, threeByName);

        resolver.setRelyingPartyConfigurations(rpConfigs);
        resolver.initialize();
        
        results = resolver.resolve(new CriteriaSet(new EntityIdCriterion("rp1"), new VerifiedProfileCriterion(true)));
        Assert.assertNotNull(results);

        Iterator<RelyingPartyConfiguration> resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), oneByName);
        Assert.assertFalse(resultItr.hasNext());

        result = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion("rp2"), new VerifiedProfileCriterion(true)));
        Assert.assertSame(result, twoByName);
        
        result = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion("doesNotExist"), new VerifiedProfileCriterion(true)));
        Assert.assertSame(result, defaultRP);
        
        results = resolver.resolve(null);
        Assert.assertNotNull(results);
        Assert.assertFalse(results.iterator().hasNext());

        result = resolver.resolveSingle(null);
        Assert.assertNull(result);
    }
    
    @Test
    public void testResolveByEntityIDViaRoleDescriptor() throws ComponentInitializationException, ResolverException {
        Iterable<RelyingPartyConfiguration> results = null;
        RelyingPartyConfiguration result = null;
        
        final List<RelyingPartyConfiguration> rpConfigs = CollectionSupport.listOf(oneByName, twoByName, threeByName);

        resolver.setRelyingPartyConfigurations(rpConfigs);
        resolver.initialize();
        
        EntityDescriptor ed = (EntityDescriptor) XMLObjectSupport.buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        ed.setEntityID("rp3");
        RoleDescriptor rd = (RoleDescriptor) XMLObjectSupport.buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        ed.getRoleDescriptors().add(rd);
        
        results = resolver.resolve(new CriteriaSet(new RoleDescriptorCriterion(rd), new VerifiedProfileCriterion(true)));
        Assert.assertNotNull(results);
        
        Iterator<RelyingPartyConfiguration> resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), threeByName);
        Assert.assertFalse(resultItr.hasNext());
        
        result = resolver.resolveSingle(new CriteriaSet(new RoleDescriptorCriterion(rd), new VerifiedProfileCriterion(true)));
        Assert.assertSame(result, threeByName);
    }
    
    @Test
    public void testResolveByEntityGroupName() throws ComponentInitializationException, ResolverException {
        Iterable<RelyingPartyConfiguration> results = null;
        RelyingPartyConfiguration result = null;
        
        assert oneByGroup!=null & twoByGroup!=null;
        final List<RelyingPartyConfiguration> rpConfigs = CollectionSupport.listOf(oneByGroup, twoByGroup);

        resolver.setRelyingPartyConfigurations(rpConfigs);
        resolver.initialize();
        
        EntityDescriptor ed = (EntityDescriptor) XMLObjectSupport.buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        ed.setEntityID("rp3");
        ed.getObjectMetadata().put(new EntityGroupName("group1"));
        RoleDescriptor rd = (RoleDescriptor) XMLObjectSupport.buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        ed.getRoleDescriptors().add(rd);
        
        results = resolver.resolve(new CriteriaSet(new RoleDescriptorCriterion(rd), new VerifiedProfileCriterion(true)));
        Assert.assertNotNull(results);
        
        Iterator<RelyingPartyConfiguration> resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), oneByGroup);
        Assert.assertFalse(resultItr.hasNext());
        
        result = resolver.resolveSingle(new CriteriaSet(new RoleDescriptorCriterion(rd), new VerifiedProfileCriterion(true)));
        Assert.assertSame(result, oneByGroup);
        
        // With 2 known group names and 1 unknown, should resolve 2 by group
        ed.getObjectMetadata().put(new EntityGroupName("group2"));
        ed.getObjectMetadata().put(new EntityGroupName("unknown"));
        
        results = resolver.resolve(new CriteriaSet(new RoleDescriptorCriterion(rd), new VerifiedProfileCriterion(true)));
        Assert.assertNotNull(results);
        
        resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), oneByGroup);
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), twoByGroup);
        Assert.assertFalse(resultItr.hasNext());
    }
    
    @Test
    public void testResolveByEntityTag() throws ComponentInitializationException, ResolverException, MarshallingException {
        Iterable<RelyingPartyConfiguration> results = null;
        RelyingPartyConfiguration result = null;
        
        assert oneByTag!=null & twoByTag!=null;
        final List<RelyingPartyConfiguration> rpConfigs = CollectionSupport.listOf(oneByTag, twoByTag);

        resolver.setRelyingPartyConfigurations(rpConfigs);
        resolver.initialize();
        
        EntityDescriptor ed = (EntityDescriptor) XMLObjectSupport.buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        ed.setEntityID("rp3");
        RoleDescriptor rd = (RoleDescriptor) XMLObjectSupport.buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        ed.getRoleDescriptors().add(rd);
        
        addTag(ed, "tag1");
        
        results = resolver.resolve(new CriteriaSet(new RoleDescriptorCriterion(rd), new VerifiedProfileCriterion(true)));
        Assert.assertNotNull(results);
        
        Iterator<RelyingPartyConfiguration> resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), oneByTag);
        Assert.assertFalse(resultItr.hasNext());
        
        result = resolver.resolveSingle(new CriteriaSet(new RoleDescriptorCriterion(rd), new VerifiedProfileCriterion(true)));
        Assert.assertSame(result, oneByTag);
        
        // With 2 known tags names and 1 unknown, should resolve 2 by tag
        addTag(ed, "tag2");
        addTag(ed, "unknown");
        
        results = resolver.resolve(new CriteriaSet(new RoleDescriptorCriterion(rd), new VerifiedProfileCriterion(true)));
        Assert.assertNotNull(results);
        
        resultItr = results.iterator();
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), oneByTag);
        Assert.assertTrue(resultItr.hasNext());
        Assert.assertSame(resultItr.next(), twoByTag);
        Assert.assertFalse(resultItr.hasNext());
    }
    
    private void addTag(EntityDescriptor ed, String value) {
        Extensions extensions = ed.getExtensions();
        if (extensions == null) {
            extensions = (Extensions) XMLObjectSupport.buildXMLObject(Extensions.DEFAULT_ELEMENT_NAME);
            ed.setExtensions(extensions);
        }
        
        EntityAttributes entityAttributes = null;
        List<XMLObject> entityAttributesList = extensions.getUnknownXMLObjects(EntityAttributes.DEFAULT_ELEMENT_NAME);
        if (entityAttributesList.isEmpty()) {
            entityAttributes = (EntityAttributes) XMLObjectSupport.buildXMLObject(EntityAttributes.DEFAULT_ELEMENT_NAME);
            extensions.getUnknownXMLObjects().add(entityAttributes);
        } else {
            entityAttributes = (EntityAttributes) entityAttributesList.get(0);
        }
        
        Attribute attr = null;
        List<Attribute> attrs = entityAttributes.getAttributes();
        if (attrs.isEmpty()) {
            attr = (Attribute) XMLObjectSupport.buildXMLObject(Attribute.DEFAULT_ELEMENT_NAME);
            attr.setNameFormat(Attribute.URI_REFERENCE);
            attr.setName("urn:test:attr:tag");
        } else {
            attr = attrs.get(0);
        }
        
        XMLObjectBuilder<?> builder = XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(XSString.TYPE_NAME);
        assert builder!=null;
        XSString val = (XSString) builder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        val.setValue(value);
        attr.getAttributeValues().add(val);
        
        attrs.add(attr);
    }

}