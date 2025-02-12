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

package net.shibboleth.idp.consent.context.impl;

import java.util.HashMap;
import java.util.Map;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.consent.context.AttributeReleaseContext;
import net.shibboleth.shared.collection.CollectionSupport;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link AttributeReleaseContext} unit test. */
@SuppressWarnings("javadoc")
public class AttributeReleaseContextTest {

    private AttributeReleaseContext ctx;

    private Map<String, IdPAttribute> map;

    @BeforeMethod public void setUp() {
        ctx = new AttributeReleaseContext();
    }

    @Test public void testInstantiation() {
        Assert.assertTrue(ctx.getConsentableAttributes().isEmpty());
    }

    @Test public void testConsentableAttributes() {
        final IdPAttribute attr1 = new IdPAttribute("attr1");
        attr1.setValues(CollectionSupport.singletonList(new StringAttributeValue("value1")));

        final IdPAttribute attr2 = new IdPAttribute("attr2");
        attr2.setValues(CollectionSupport.singletonList(new StringAttributeValue("value2")));

        map = new HashMap<>();
        map.put(attr1.getId(), attr1);
        map.put(attr2.getId(), attr2);
        
        ctx.getConsentableAttributes().putAll(map);
        
        Assert.assertEquals(map, ctx.getConsentableAttributes());
    }
}
