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

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterPolicy;
import net.shibboleth.idp.attribute.filter.AttributeRule;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.impl.AttributeFilterImpl;
import net.shibboleth.idp.attribute.filter.testing.MockMatcher;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.impl.FilterAttributes;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.MockReloadableService;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link FilterAttributes} unit test. */
public class FilterAttributesTest {

    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    /**
     * Set up tests.
     * 
     * @throws ComponentInitializationException
     */
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
    }
    
    /**
     * Test that the action proceeds properly if there is no attribute context.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoAttributeContext() throws Exception {
        prc.ensureSubcontext(SubjectContext.class);

        final AttributeFilterImpl engine = new AttributeFilterImpl("test", CollectionSupport.emptyList());
        engine.initialize();

        final FilterAttributes action = new FilterAttributes(new MockReloadableService<>(engine));
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

    /**
     * Test that the action proceeds properly if there are no attributes to filter.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoAttributes() throws Exception {
        prc.ensureSubcontext(SubjectContext.class);

        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!= null;
        rpCtx.ensureSubcontext(AttributeContext.class);

        final AttributeFilterImpl engine = new AttributeFilterImpl("test", CollectionSupport.emptyList());
        engine.initialize();
        
        final FilterAttributes action = new FilterAttributes(new MockReloadableService<>(engine));
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

    /**
     * Test that the action filters attributes and proceeds properly while auto-creating a filter context.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testFilterAttributesAutoCreateFilterContext() throws Exception {
        final IdPAttribute attribute1 = new IdPAttribute("attribute1");
        attribute1.setValues(CollectionSupport.listOf(new StringAttributeValue("one"), new StringAttributeValue("two")));

        final IdPAttribute attribute2 = new IdPAttribute("attribute2");
        attribute2.setValues(CollectionSupport.listOf(new StringAttributeValue("a"), new StringAttributeValue("b")));

        final List<IdPAttribute> attributes = CollectionSupport.listOf(attribute1, attribute2);

        final MockMatcher attribute1Matcher = new MockMatcher();
        attribute1Matcher.setMatchingAttribute("attribute1");
        attribute1Matcher.setMatchingValues(null);

        final AttributeRule attribute1Policy = new AttributeRule();
        attribute1Policy.setId("attribute1Policy");
        attribute1Policy.setAttributeId("attribute1");
        attribute1Policy.setMatcher(attribute1Matcher);
        attribute1Policy.setIsDenyRule(false);

        final AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", PolicyRequirementRule.MATCHES_ALL,
                        CollectionSupport.singletonList(attribute1Policy));

        final AttributeFilterImpl engine = new AttributeFilterImpl("engine", CollectionSupport.singletonList(policy));
        policy.initialize();
        attribute1Policy.initialize();
        attribute1Matcher.initialize();
        engine.initialize();

        prc.ensureSubcontext(SubjectContext.class);

        final AttributeContext attributeCtx = new AttributeContext();
        attributeCtx.setIdPAttributes(attributes);
        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!= null;
        rpCtx.addSubcontext(attributeCtx);

        final FilterAttributes action = new FilterAttributes(new MockReloadableService<>(engine));
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        // The attribute filter context should be removed by the filter attributes action.
        Assert.assertNull(rpCtx.getSubcontext(
                AttributeFilterContext.class));

        final AttributeContext resultAttributeCtx = rpCtx.getSubcontext(AttributeContext.class);
        assert resultAttributeCtx!=null;

        final Map<String, IdPAttribute> resultAttributes = resultAttributeCtx.getIdPAttributes();
        Assert.assertEquals(resultAttributes.size(), 1);

        final List<IdPAttributeValue> resultAttributeValue = resultAttributes.get("attribute1").getValues();
        Assert.assertEquals(resultAttributeValue.size(), 2);
        Assert.assertTrue(resultAttributeValue.contains(new StringAttributeValue("one")));
        Assert.assertTrue(resultAttributeValue.contains(new StringAttributeValue("two")));
    }

    /**
     * Test that the action filters attributes and proceeds properly with an existing filter context.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testFilterAttributesExistingFilterContext() throws Exception {
        final IdPAttribute attribute1 = new IdPAttribute("attribute1");
        attribute1.setValues(CollectionSupport.listOf(new StringAttributeValue("one"), new StringAttributeValue("two")));

        final IdPAttribute attribute2 = new IdPAttribute("attribute2");
        attribute2.setValues(CollectionSupport.listOf(new StringAttributeValue("a"), new StringAttributeValue("b")));

        final List<IdPAttribute> attributes = CollectionSupport.listOf(attribute1, attribute2);

        final MockMatcher attribute1Matcher = new MockMatcher();
        attribute1Matcher.setMatchingAttribute("attribute1");
        attribute1Matcher.setMatchingValues(null);

        final AttributeRule attribute1Policy = new AttributeRule();
        attribute1Policy.setId("attribute1Policy");
        attribute1Policy.setAttributeId("attribute1");
        attribute1Policy.setMatcher(attribute1Matcher);
        attribute1Policy.setIsDenyRule(false);

        final AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", PolicyRequirementRule.MATCHES_ALL,
                        CollectionSupport.singletonList(attribute1Policy));

        final AttributeFilterImpl engine = new AttributeFilterImpl("engine", CollectionSupport.singletonList(policy));
        policy.initialize();
        attribute1Policy.initialize();
        attribute1Matcher.initialize();
        engine.initialize();

        prc.ensureSubcontext(SubjectContext.class);

        final AttributeContext attributeCtx = new AttributeContext();
        attributeCtx.setIdPAttributes(attributes);
        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!= null;
        rpCtx.addSubcontext(attributeCtx);

        final AttributeFilterContext attributeFilterCtx = new AttributeFilterContext();
        rpCtx.addSubcontext(attributeFilterCtx);

        final FilterAttributes action = new FilterAttributes(new MockReloadableService<>(engine));
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        // The attribute filter context should be removed by the filter attributes action.
        Assert.assertNull(rpCtx.getSubcontext(AttributeFilterContext.class));

        final AttributeContext resultAttributeCtx = rpCtx.getSubcontext(AttributeContext.class);
        assert resultAttributeCtx!=null;

        final Map<String, IdPAttribute> resultAttributes = resultAttributeCtx.getIdPAttributes();
        Assert.assertEquals(resultAttributes.size(), 1);

        final List<IdPAttributeValue> resultAttributeValue = resultAttributes.get("attribute1").getValues();
        Assert.assertEquals(resultAttributeValue.size(), 2);
        Assert.assertTrue(resultAttributeValue.contains(new StringAttributeValue("one")));
        Assert.assertTrue(resultAttributeValue.contains(new StringAttributeValue("two")));
    }

    /**
     * Test that action returns the proper event if the attributes are not able to be filtered.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testUnableToFilterAttributes() throws Exception {
        final IdPAttribute attribute1 = new MockUncloneableAttribute("attribute1");
        attribute1.setValues(CollectionSupport.listOf(new StringAttributeValue("one"), new StringAttributeValue("two")));

        final List<IdPAttribute> attributes = CollectionSupport.singletonList(attribute1);

        final MockMatcher attribute1Matcher = new MockMatcher();
        attribute1Matcher.setMatchingAttribute("attribute1");
        attribute1Matcher.setMatchingValues(null);

        final AttributeRule attribute1Policy = new AttributeRule();
        attribute1Policy.setId("attribute1Policy");
        attribute1Policy.setAttributeId("attribute1");
        attribute1Policy.setMatcher(attribute1Matcher);
        attribute1Policy.setIsDenyRule(false);

        final AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", PolicyRequirementRule.MATCHES_ALL,
                        CollectionSupport.singletonList(attribute1Policy));

        final AttributeFilterImpl engine = new AttributeFilterImpl("engine", CollectionSupport.singletonList(policy));
        policy.initialize();
        attribute1Policy.initialize();
        attribute1Matcher.initialize();
        engine.initialize();

        prc.ensureSubcontext(SubjectContext.class);

        final AttributeContext attributeCtx = new AttributeContext();
        attributeCtx.setIdPAttributes(attributes);
        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!= null;
        rpCtx.addSubcontext(attributeCtx);

        final AttributeFilterContext attributeFilterCtx = new AttributeFilterContext();
        rpCtx.addSubcontext(attributeFilterCtx);

        final FilterAttributes action = new FilterAttributes(new MockReloadableService<>(engine));
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertTrue(attributeCtx.getIdPAttributes().isEmpty());
    }
    
    /**
     * Test that action returns the proper event if the attribute configuration is broken.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testUnableToFindFilter() throws Exception {
        final IdPAttribute attribute1 = new MockUncloneableAttribute("attribute1");
        attribute1.setValues(CollectionSupport.listOf(new StringAttributeValue("one"), new StringAttributeValue("two")));

        prc.ensureSubcontext(SubjectContext.class);

        final AttributeContext attributeCtx = new AttributeContext();
        final List<IdPAttribute> attributes = CollectionSupport.singletonList(attribute1);
        attributeCtx.setIdPAttributes(attributes);
        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!= null;
        rpCtx.addSubcontext(attributeCtx);

        final AttributeFilterContext attributeFilterCtx = new AttributeFilterContext();
        rpCtx.addSubcontext(attributeFilterCtx);

        final FilterAttributes action = new FilterAttributes(new MockReloadableService<>(null));
        action.setMaskFailures(false);
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, IdPEventIds.UNABLE_FILTER_ATTRIBS);
    }


    /** {@link IdPAttribute} which always throws a {@link CloneNotSupportedException}. */
    private class MockUncloneableAttribute extends IdPAttribute {

        /**
         * Constructor.
         * 
         * @param attributeId ...
         */
        public MockUncloneableAttribute(@Nonnull String attributeId) {
            super(attributeId);
        }

        /** Always throws exception. */
        @Override
        public @Nonnull IdPAttribute clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
    }
    
}
