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

package net.shibboleth.idp.cas.attribute.transcoding.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.BasicNamingFunction;
import net.shibboleth.idp.attribute.transcoding.TranscoderSupport;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.attribute.transcoding.impl.AttributeTranscoderRegistryImpl;
import net.shibboleth.idp.cas.attribute.AbstractCASAttributeTranscoder;
import net.shibboleth.idp.cas.attribute.Attribute;
import net.shibboleth.idp.cas.attribute.CASAttributeTranscoder;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.MockApplicationContext;

/** {@link CASStringAttributeTranscoder} unit test. */
@SuppressWarnings("javadoc")
public class CASStringAttributeTranscoderTest {

    private AttributeTranscoderRegistryImpl registry;
    
    private final static String ATTR_ID = "foo";
    private final static String ATTR_NAME = "bar";
    private final static String STRING_1 = "Value The First";
    private final static String STRING_2 = "Second string the value is";

    @BeforeClass public void setUp() throws ComponentInitializationException {
        
        registry = new AttributeTranscoderRegistryImpl();
        registry.setId("test");
                
        final CASStringAttributeTranscoder transcoder = new CASStringAttributeTranscoder();
        transcoder.initialize();
        
        registry.setNamingRegistry(CollectionSupport.singletonList(
                new BasicNamingFunction<>(transcoder.getEncodedType(), new AbstractCASAttributeTranscoder.NamingFunction())));
        
        final Map<String,Object> ruleset1 = new HashMap<>();
        ruleset1.put(AttributeTranscoderRegistry.PROP_ID, ATTR_ID);
        ruleset1.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        ruleset1.put(CASAttributeTranscoder.PROP_NAME, ATTR_NAME);
        
        registry.setTranscoderRegistry(CollectionSupport.singletonList(new TranscodingRule(ruleset1)));
        registry.setApplicationContext(new MockApplicationContext());
        registry.initialize();
    }
    
    @AfterClass public void tearDown() {
        registry.destroy();
        registry = null;
    }

    @Test public void emptyEncode() throws Exception {
        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_ID);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        assert ruleset != null;

        final Attribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(
                null, inputAttribute, Attribute.class, ruleset);
        assert attr!= null;
        Assert.assertEquals(attr.getName(), ATTR_NAME);
        Assert.assertTrue(attr.getValues().isEmpty());
    }

    @Test public void emptyDecode() throws Exception {
        
        final Attribute casAttribute = new Attribute(ATTR_NAME);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(casAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        assert ruleset!=null;
        final IdPAttribute attr = TranscoderSupport.getTranscoder(ruleset).decode(null, casAttribute, ruleset);
        
        assert attr!= null;
        Assert.assertEquals(attr.getId(), ATTR_ID);
        Assert.assertTrue(attr.getValues().isEmpty());
    }

    @Test(expectedExceptions = {AttributeEncodingException.class,}) public void inappropriate() throws Exception {
        final int[] intArray = {1, 2, 3, 4};
        final List<IdPAttributeValue> values =
                CollectionSupport.listOf(new ByteAttributeValue(new byte[] {1, 2, 3,}), new IdPAttributeValue() {
                    @Override
                    public @Nonnull Object getNativeValue() {
                        return intArray;
                    }
                    @Override
                    public @Nonnull String getDisplayValue() {
                        final String result = intArray.toString();
                        assert result!= null;
                        return result;
                    }
                });

        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_ID);
        inputAttribute.setValues(values);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        assert ruleset != null;

        TranscoderSupport.getTranscoder(ruleset).encode(null, inputAttribute, Attribute.class, ruleset);
    }
    
    @Test public void single() throws Exception {
        final List<IdPAttributeValue> values =
                CollectionSupport.listOf(new ByteAttributeValue(new byte[] {1, 2, 3,}), new StringAttributeValue(STRING_1));

        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_ID);
        inputAttribute.setValues(values);
        
        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        assert ruleset != null;

        final Attribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(
                null, inputAttribute, Attribute.class, ruleset);

        assert attr!= null;
        Assert.assertEquals(attr.getName(), ATTR_NAME);

        final Collection<String> children = attr.getValues();

        Assert.assertEquals(children.size(), 1, "Encoding one entry");

        final String child = children.iterator().next();

        Assert.assertEquals(child, STRING_1);
    }
    
    @Test public void singleDecode() throws Exception {
        
        final Attribute casAttribute = new Attribute(ATTR_NAME);
        casAttribute.getValues().add(STRING_1);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(casAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        assert ruleset != null;

        final IdPAttribute attr = TranscoderSupport.getTranscoder(ruleset).decode(null, casAttribute, ruleset);
        
        assert attr!= null;
        Assert.assertEquals(attr.getId(), ATTR_ID);
        Assert.assertEquals(attr.getValues().size(), 1);
        Assert.assertEquals(((StringAttributeValue)attr.getValues().get(0)).getValue().toString(), STRING_1);
    }
        
    @Test public void multi() throws Exception {
        final List<IdPAttributeValue> values =
                CollectionSupport.listOf(new ByteAttributeValue(new byte[] {1, 2, 3,}),
                        new StringAttributeValue(STRING_1),
                        new StringAttributeValue(STRING_2),
                        new ScopedStringAttributeValue(STRING_1, STRING_2));

        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_ID);
        inputAttribute.setValues(values);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        assert ruleset!=null;
        final Attribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(
                null, inputAttribute, Attribute.class, ruleset);

        assert attr!= null;
        Assert.assertEquals(attr.getName(), ATTR_NAME);

        final Collection<String> children = attr.getValues();
        Assert.assertEquals(children.size(), 3, "Encoding three entries");

        for (final String child: children) {
            Assert.assertTrue(STRING_1.equals(child)||STRING_2.equals(child));
        }
    }

    @Test public void multiDecode() throws Exception {
        
        final Attribute casAttribute = new Attribute(ATTR_NAME);
        casAttribute.getValues().add(STRING_1);
        casAttribute.getValues().add(STRING_2);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(casAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        assert ruleset != null;

        final IdPAttribute attr = TranscoderSupport.getTranscoder(ruleset).decode(null, casAttribute, ruleset);
        
        assert attr!= null;
        Assert.assertEquals(attr.getId(), ATTR_ID);
        Assert.assertEquals(attr.getValues().size(), 2);
        Assert.assertEquals(((StringAttributeValue)attr.getValues().get(0)).getValue().toString(), STRING_1);
        Assert.assertEquals(((StringAttributeValue)attr.getValues().get(1)).getValue().toString(), STRING_2);
    }

}
