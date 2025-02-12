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

package net.shibboleth.idp.saml.nameid.impl;

import javax.sql.DataSource;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.testing.SAML2ActionTestingSupport;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.PairwiseId;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.impl.ComputedPairwiseIdStore;
import net.shibboleth.idp.attribute.impl.JDBCPairwiseIdStore;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.impl.testing.TestSources;
import net.shibboleth.idp.saml.saml2.profile.config.impl.BrowserSSOProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.DatabaseTestingSupport;

/**
 * Test for {@link PersistentSAML2NameIDGenerator}.
 */
@SuppressWarnings({"javadoc", "null"})
public class PersistentSAML2NameIDGeneratorTest extends OpenSAMLInitBaseTestCase {

    /** Value calculated using V2 version. DO NOT CHANGE WITHOUT TESTING AGAINST 2.0 */
    private static final String RESULT = "Vl6z6K70iLc4AuBoNeb59Dj1rGw=";

    private static final byte salt[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    public static final String INIT_FILE = "/net/shibboleth/idp/saml/impl/nameid/StoredIdStore.sql";
    public static final String DELETE_FILE = "/net/shibboleth/idp/saml/impl/nameid/DeleteStore.sql";
    

    private DataSource testSource;
    
    private ProfileRequestContext prc;
    
    private PersistentSAML2NameIDGenerator generator;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        testSource = DatabaseTestingSupport.GetMockDataSource(INIT_FILE, "StoredIDDataConnectorStore");
        prc = new RequestContextBuilder()
                .setInboundMessageIssuer(TestSources.SP_ENTITY_ID)
                .setOutboundMessageIssuer(TestSources.IDP_ENTITY_ID)
                .setRelyingPartyProfileConfigurations(CollectionSupport.singletonList(new BrowserSSOProfileConfiguration()))
                .buildProfileRequestContext();
        generator = new PersistentSAML2NameIDGenerator();
        generator.setId("test");
        generator.setOmitQualifiers(false);
    }
    
    @AfterMethod
    public void tearDown() {
        DatabaseTestingSupport.InitializeDataSource(DELETE_FILE, testSource);
    }
    
    @Test(expectedExceptions = ComponentInitializationException.class)
    public void testInvalidConfig() throws ComponentInitializationException {
        final ComputedPairwiseIdStore strategy = new ComputedPairwiseIdStore();
        strategy.setSalt(salt);
        strategy.initialize();
        
        generator.initialize();
        generator.setPersistentIdStore(strategy);
        generator.initialize();
    }

    @Test
    public void testNoResponderId() throws Exception {
        generator.setPersistentIdStore(new ComputedPairwiseIdStore());
        generator.setAttributeSourceIds(CollectionSupport.singletonList("SOURCE"));
        generator.initialize();
        
        Assert.assertNull(generator.generate(new ProfileRequestContext(), NameID.PERSISTENT));
    }

    @Test
    public void testNoRequesterId() throws Exception {
        generator.setPersistentIdStore(new ComputedPairwiseIdStore());
        generator.setAttributeSourceIds(CollectionSupport.singletonList("SOURCE"));
        generator.initialize();
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class);
        assert rpc!=null;
        rpc.setRelyingPartyId(null);
        
        Assert.assertNull(generator.generate(prc, NameID.PERSISTENT));
    }
    
    @Test
    public void testNoSubject() throws Exception {
        generator.setPersistentIdStore(new ComputedPairwiseIdStore());
        generator.setAttributeSourceIds(CollectionSupport.singletonList("SOURCE"));
        generator.initialize();
        
        Assert.assertNull(generator.generate(prc, NameID.PERSISTENT));
    }

    @Test
    public void testNoSource() throws Exception {
        generator.setPersistentIdStore(new ComputedPairwiseIdStore());
        generator.setAttributeSourceIds(CollectionSupport.singletonList("SOURCE"));
        generator.initialize();
        
        prc.ensureSubcontext(SubjectContext.class).setPrincipalName("foo");
        Assert.assertNull(generator.generate(prc, NameID.PERSISTENT));
        
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class);
        assert rpc!=null;
        rpc.ensureSubcontext(AttributeContext.class).setUnfilteredIdPAttributes(
                CollectionSupport.singleton(new IdPAttribute("SOURCE")));
        Assert.assertNull(generator.generate(prc, NameID.PERSISTENT));
    }
    
    @Test
    public void testComputedId() throws Exception {
        final ComputedPairwiseIdStore strategy = new ComputedPairwiseIdStore();
        strategy.setSalt(salt);
        strategy.initialize();

        generator.setPersistentIdStore(strategy);
        generator.setAttributeSourceIds(CollectionSupport.singletonList("SOURCE"));
        generator.initialize();
        
        prc.ensureSubcontext(SubjectContext.class).setPrincipalName("foo");
        Assert.assertNull(generator.generate(prc, NameID.PERSISTENT));
        
        final IdPAttribute source = new IdPAttribute("SOURCE");
        source.setValues(CollectionSupport.singletonList(new StringAttributeValue(TestSources.COMMON_ATTRIBUTE_VALUE_STRING)));
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class);
        assert rpc!=null;
        rpc.ensureSubcontext(AttributeContext.class).setUnfilteredIdPAttributes(CollectionSupport.singleton(source));
        final NameID id = generator.generate(prc, NameID.PERSISTENT);
        assert id!=null;
        Assert.assertEquals(id.getValue(), RESULT);
        Assert.assertEquals(id.getFormat(), NameID.PERSISTENT);
        Assert.assertEquals(id.getNameQualifier(), TestSources.IDP_ENTITY_ID);
        Assert.assertEquals(id.getSPNameQualifier(), TestSources.SP_ENTITY_ID);
    }
    
    @Test
    public void testStoredId() throws Exception {
        generator.setDataSource(testSource);
        
        testStoredIdLogic();
    }

    @Test
    public void testComputedAndStoredId() throws Exception {
        final ComputedPairwiseIdStore strategy = new ComputedPairwiseIdStore();
        strategy.setSalt(salt);
        strategy.initialize();

        final JDBCPairwiseIdStore store = new JDBCPairwiseIdStore();
        store.setDataSource(testSource);
        store.setInitialValueStore(strategy);
        store.initialize();
        
        generator.setPersistentIdStore(store);
        
        testComputedAndStoredIdLogic();

        final PairwiseId pid = new PairwiseId();
        pid.setIssuerEntityID(TestSources.IDP_ENTITY_ID);
        pid.setRecipientEntityID(TestSources.SP_ENTITY_ID);
        pid.setPairwiseId(RESULT);
        store.deactivate(pid);
        
        final NameID id = generator.generate(prc, NameID.PERSISTENT);
        assert id != null;
        Assert.assertNotEquals(id.getValue(), RESULT);
        Assert.assertEquals(id.getNameQualifier(), TestSources.IDP_ENTITY_ID);
        Assert.assertEquals(id.getSPNameQualifier(), TestSources.SP_ENTITY_ID);
    }

    private void testStoredIdLogic() throws Exception {
        generator.setAttributeSourceIds(CollectionSupport.singletonList("SOURCE"));
        generator.initialize();
        
        prc.ensureSubcontext(SubjectContext.class).setPrincipalName("foo");
        Assert.assertNull(generator.generate(prc, NameID.PERSISTENT));
        
        final IdPAttribute source = new IdPAttribute("SOURCE");
        source.setValues(CollectionSupport.singletonList(new StringAttributeValue(TestSources.COMMON_ATTRIBUTE_VALUE_STRING)));
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class);
        assert rpc!=null;
        rpc.ensureSubcontext(AttributeContext.class).setUnfilteredIdPAttributes(
                CollectionSupport.singleton(source));
        NameID id = generator.generate(prc, NameID.PERSISTENT);
        assert id!=null;
        Assert.assertNotNull(id.getValue());
        Assert.assertEquals(id.getFormat(), NameID.PERSISTENT);
        Assert.assertEquals(id.getNameQualifier(), TestSources.IDP_ENTITY_ID);
        Assert.assertEquals(id.getSPNameQualifier(), TestSources.SP_ENTITY_ID);
        
        String storedvalue = id.getValue();
        id = generator.generate(prc, NameID.PERSISTENT);
        assert id!=null;
        Assert.assertEquals(id.getValue(), storedvalue);
        Assert.assertEquals(id.getFormat(), NameID.PERSISTENT);
        Assert.assertEquals(id.getNameQualifier(), TestSources.IDP_ENTITY_ID);
        Assert.assertEquals(id.getSPNameQualifier(), TestSources.SP_ENTITY_ID);
        
        // Set up an affiliation and divert the generator to it.
        final AuthnRequest request = SAML2ActionTestingSupport.buildAuthnRequest();
        final NameIDPolicy policy = (NameIDPolicy) XMLObjectSupport.buildXMLObject(NameIDPolicy.DEFAULT_ELEMENT_NAME);
        request.setNameIDPolicy(policy);
        policy.setSPNameQualifier("https://affiliation.org");
        MessageContext imc = prc.getInboundMessageContext();
        assert imc != null;
        imc.setMessage(request);
        
        id = generator.generate(prc, NameID.PERSISTENT);
        assert id!=null;
        Assert.assertNotNull(id.getValue());
        Assert.assertEquals(id.getFormat(), NameID.PERSISTENT);
        Assert.assertEquals(id.getNameQualifier(), TestSources.IDP_ENTITY_ID);
        Assert.assertEquals(id.getSPNameQualifier(), "https://affiliation.org");
        
        storedvalue = id.getValue();
        id = generator.generate(prc, NameID.PERSISTENT);
        assert id!=null;
        Assert.assertEquals(id.getValue(), storedvalue);
        Assert.assertEquals(id.getNameQualifier(), TestSources.IDP_ENTITY_ID);
        Assert.assertEquals(id.getSPNameQualifier(), "https://affiliation.org");
        
        imc = prc.getInboundMessageContext();
        assert imc != null;
        imc.setMessage(null);
        final RelyingPartyContext rpc2 = prc.getSubcontext(RelyingPartyContext.class);
        assert rpc2!=null;

        final BrowserSSOProfileConfiguration config =(BrowserSSOProfileConfiguration) rpc2.getProfileConfig();
        assert config!= null;
        config.setSPNameQualifier("https://affiliation.org");
        id = generator.generate(prc, NameID.PERSISTENT);
        assert id!=null;
        Assert.assertEquals(id.getValue(), storedvalue);
        Assert.assertEquals(id.getNameQualifier(), TestSources.IDP_ENTITY_ID);
        Assert.assertEquals(id.getSPNameQualifier(), "https://affiliation.org");
    }
    
    private void testComputedAndStoredIdLogic() throws Exception {
        generator.setAttributeSourceIds(CollectionSupport.singletonList("SOURCE"));
        generator.initialize();
        
        prc.ensureSubcontext(SubjectContext.class).setPrincipalName("foo");
        Assert.assertNull(generator.generate(prc, NameID.PERSISTENT));
        
        final IdPAttribute source = new IdPAttribute("SOURCE");
        source.setValues(CollectionSupport.singletonList(new StringAttributeValue(TestSources.COMMON_ATTRIBUTE_VALUE_STRING)));
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class);
        assert rpc!=null;
        rpc.ensureSubcontext(AttributeContext.class).setUnfilteredIdPAttributes(
                CollectionSupport.singleton(source));
        NameID id = generator.generate(prc, NameID.PERSISTENT);
        assert id!=null;
        Assert.assertEquals(id.getValue(), RESULT);
        Assert.assertEquals(id.getFormat(), NameID.PERSISTENT);
        Assert.assertEquals(id.getNameQualifier(), TestSources.IDP_ENTITY_ID);
        Assert.assertEquals(id.getSPNameQualifier(), TestSources.SP_ENTITY_ID);
        
        id = generator.generate(prc, NameID.PERSISTENT);
        assert id!=null;
        Assert.assertEquals(id.getValue(), RESULT);
        Assert.assertEquals(id.getNameQualifier(), TestSources.IDP_ENTITY_ID);
        Assert.assertEquals(id.getSPNameQualifier(), TestSources.SP_ENTITY_ID);
    }
    
}