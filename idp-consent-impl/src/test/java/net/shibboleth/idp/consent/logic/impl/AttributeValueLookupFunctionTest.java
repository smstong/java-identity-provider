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

package net.shibboleth.idp.consent.logic.impl;

import java.util.Map;

import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.consent.impl.ConsentTestingSupport;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.ConstraintViolationException;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link AttributeValueLookupFunction} unit test. */
@SuppressWarnings("javadoc")
public class AttributeValueLookupFunctionTest {

    private AttributeValueLookupFunction function;

    private RequestContext src;

    private ProfileRequestContext prc;
    
    private String nullObj;

    @BeforeMethod public void setUp() throws Exception {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        final AttributeContext attributeCtx = new AttributeContext();
        
        final Map<String, IdPAttribute> attributes = ConsentTestingSupport.newAttributeMap();
        attributeCtx.setIdPAttributes(attributes.values());
        
        final Map<String, IdPAttribute> unfilteredAttributes = ConsentTestingSupport.newAttributeMap();
        final IdPAttribute attribute4 = new IdPAttribute("attribute4");
        attribute4.setValues(CollectionSupport.singletonList(new StringAttributeValue("value4")));
        unfilteredAttributes.put(attribute4.getId(), attribute4);
        attributeCtx.setUnfilteredIdPAttributes(unfilteredAttributes.values());
        
        prc.ensureSubcontext(RelyingPartyContext.class).addSubcontext(attributeCtx);
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testEmptyConstructor() {
        function = new AttributeValueLookupFunction("");
    }

    @SuppressWarnings("null")
    @Test(expectedExceptions = ConstraintViolationException.class) public void testNullConstructor() {
        function = new AttributeValueLookupFunction(nullObj);
    }

    @Test public void testNullProfileRequestContext() {
        function = new AttributeValueLookupFunction("foo");

        Assert.assertNull(function.apply(null));
    }

    @Test public void testAttributeValue() {
        function = new AttributeValueLookupFunction("attribute1");
        Assert.assertEquals(function.apply(prc), "Avalue1");
    }

    @Test public void testAttributeNotFound() {
        function = new AttributeValueLookupFunction("notFound");

        Assert.assertNull(function.apply(prc));
    }

    @Test public void testAttributeWithNoValues() {
        final AttributeContext attributeCtx =
                prc.ensureSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class);
        assert attributeCtx!=null;
        attributeCtx.setIdPAttributes(CollectionSupport.singleton(new IdPAttribute("EmptyAttribute")));

        function = new AttributeValueLookupFunction("EmptyAttribute");
        Assert.assertNull(function.apply(prc));
    }

    @Test public void testNonStringAttributeValue() {
        byte[] data = {1, 2, 3, 0xF};

        final IdPAttribute byteAttribute = new IdPAttribute("ByteAttribute");
        byteAttribute.setValues(CollectionSupport.singletonList(new ByteAttributeValue(data)));

        final AttributeContext attributeCtx =
                prc.ensureSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class);
        assert attributeCtx!=null;
        attributeCtx.setIdPAttributes(CollectionSupport.singleton(byteAttribute));

        function = new AttributeValueLookupFunction("ByteAttribute");
        Assert.assertNull(function.apply(prc));
    }

    @Test public void testUseFilteredAttributes() {
        function = new AttributeValueLookupFunction("attribute4");
        function.setUseUnfilteredAttributes(false);
        Assert.assertNull(function.apply(prc));
    }

    @Test public void testUseUnfilteredAttributes() {
        function = new AttributeValueLookupFunction("attribute4");
        Assert.assertEquals(function.apply(prc), "value4");
    }
}
