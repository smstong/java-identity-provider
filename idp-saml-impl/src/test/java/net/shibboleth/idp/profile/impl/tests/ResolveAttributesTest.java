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

package net.shibboleth.idp.profile.impl.tests;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImpl;
import net.shibboleth.idp.attribute.resolver.testing.MockAttributeDefinition;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.impl.ResolveAttributes;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.collection.LazySet;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.MockReloadableService;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link ResolveAttributes} unit test. */
public class ResolveAttributesTest {

    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    /**
     * Test setup.
     * 
     * @throws ComponentInitializationException
     */
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
    }
    
    /**
     * Test that the action resolves attributes and proceeds properly.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testResolveAttributes() throws Exception {
        prc.ensureSubcontext(SubjectContext.class);

        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.setValues(CollectionSupport.singletonList(new StringAttributeValue("value1")));

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        final AttributeDefinition ad1 = new MockAttributeDefinition("ad1", attribute);
        definitions.add(ad1);

        final AttributeResolverImpl resolver = newAttributeResolverImpl("resolver", definitions, null);
        ad1.initialize();
        resolver.initialize();

        final ResolveAttributes action = new ResolveAttributes(new MockReloadableService<>(resolver));
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        // The attribute resolution context should be removed by the resolve attributes action.
        Assert.assertNull(prc.getSubcontext(AttributeResolutionContext.class));

        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!= null;
        final AttributeContext resolvedAttributeCtx =
                rpCtx.getSubcontext(AttributeContext.class);
        assert resolvedAttributeCtx!=null;

        final Map<String, IdPAttribute> resolvedAttributes = resolvedAttributeCtx.getIdPAttributes();
        Assert.assertFalse(resolvedAttributes.isEmpty());
        Assert.assertEquals(resolvedAttributes.size(), 1);
        Assert.assertNotNull(resolvedAttributes.get("ad1"));
        Assert.assertEquals(resolvedAttributes.get("ad1"), attribute);
    }

    /**
     * Test that the action resolves specific attributes and proceeds properly.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testResolveSpecificAttributes() throws Exception {
        prc.ensureSubcontext(SubjectContext.class);

        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.setValues(CollectionSupport.singletonList(new StringAttributeValue("value1")));

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        final AttributeDefinition ad1 = new MockAttributeDefinition("ad1", attribute);
        definitions.add(ad1);

        final AttributeResolverImpl resolver = newAttributeResolverImpl("resolver", definitions, null);
        ad1.initialize();
        resolver.initialize();

        AttributeResolutionContext attributeResolutionCtx = new AttributeResolutionContext();
        attributeResolutionCtx.setRequestedIdPAttributeNames(CollectionSupport.singleton("ad1"));
        prc.addSubcontext(attributeResolutionCtx);

        final ResolveAttributes action = new ResolveAttributes(new MockReloadableService<>(resolver));
        action.initialize();

        Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        // The attribute resolution context should be removed by the resolve attributes action.
        Assert.assertNull(prc.getSubcontext(AttributeResolutionContext.class));

        RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!= null;
        AttributeContext resolvedAttributeCtx = rpCtx.getSubcontext(AttributeContext.class);
        assert resolvedAttributeCtx != null;

        final Map<String, IdPAttribute> resolvedAttributes = resolvedAttributeCtx.getIdPAttributes();
        Assert.assertFalse(resolvedAttributes.isEmpty());
        Assert.assertEquals(resolvedAttributes.size(), 1);
        Assert.assertNotNull(resolvedAttributes.get("ad1"));
        Assert.assertEquals(resolvedAttributes.get("ad1"), attribute);

        // now test requesting an attribute that does not exist
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        prc.ensureSubcontext(SubjectContext.class);

        attributeResolutionCtx = new AttributeResolutionContext();
        attributeResolutionCtx.setRequestedIdPAttributeNames(CollectionSupport.singleton("dne"));
        prc.addSubcontext(attributeResolutionCtx, true);

        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        // The attribute resolution context should be removed by the resolve attributes action.
        Assert.assertNull(prc.getSubcontext(AttributeResolutionContext.class));

        rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!= null;
        resolvedAttributeCtx = rpCtx.getSubcontext(AttributeContext.class);
        assert resolvedAttributeCtx != null;
        Assert.assertTrue(resolvedAttributeCtx.getIdPAttributes().isEmpty());
    }

    /**
     * Test that action returns the proper event if the attributes are not able to be resolved.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testUnableToResolveAttributes() throws Exception {
        prc.ensureSubcontext(SubjectContext.class);

        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.setValues(CollectionSupport.singletonList(new StringAttributeValue("value1")));

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        final AttributeDefinition ad1 = new MockAttributeDefinition("ad1", new ResolutionException());
        definitions.add(ad1);

        final AttributeResolverImpl resolver = newAttributeResolverImpl("resolver", definitions, null);
        ad1.initialize();
        resolver.initialize();

        final ResolveAttributes action = new ResolveAttributes(new MockReloadableService<>(resolver));
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!= null;
        final AttributeContext resolvedAttributeCtx = rpCtx.getSubcontext(AttributeContext.class);
        Assert.assertNull(resolvedAttributeCtx);
    }
    
    /**
     * Test that action returns the proper event if the attribute configuration is broken.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testUnableToFindResolver() throws Exception {
        prc.ensureSubcontext(SubjectContext.class);

        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.setValues(CollectionSupport.singletonList(new StringAttributeValue("value1")));

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(new MockAttributeDefinition("ad1", new ResolutionException()));

        final ResolveAttributes action = new ResolveAttributes(new MockReloadableService<>(null));
        action.setMaskFailures(false);
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, IdPEventIds.UNABLE_RESOLVE_ATTRIBS);
    }
    
    /**
     * Create the resolver.
     * 
     * @param resolverId resolver ID
     * @param definitions attribute definitions
     * @param connectors data connectors
     * 
     * @return the resolver implementation
     */
    public static AttributeResolverImpl newAttributeResolverImpl(@Nonnull @NotEmpty final String resolverId,
            @Nullable final Collection<AttributeDefinition> definitions,
            @Nullable final Collection<DataConnector> connectors) {
        final AttributeResolverImpl result = new AttributeResolverImpl();
        result.setId(resolverId);
        
        result.setAttributeDefinitions(definitions == null ? CollectionSupport.emptyList() : definitions);
        result.setDataConnectors(connectors == null ? CollectionSupport.emptyList() : connectors);
        return result;
    }

}