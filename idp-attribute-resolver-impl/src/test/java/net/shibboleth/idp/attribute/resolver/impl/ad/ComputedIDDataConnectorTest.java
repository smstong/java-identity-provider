/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImpl;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.idp.saml.impl.attribute.resolver.AbstractComputedIDDataConnector;
import net.shibboleth.idp.saml.impl.attribute.resolver.ComputedIDDataConnector;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for {@link AbstractComputedIDDataConnector}
 */
public class ComputedIDDataConnectorTest extends OpenSAMLInitBaseTestCase {

    /** The attribute name. */
    private static final String TEST_ATTRIBUTE_NAME = "computedAttribute";

    /** The connector name. */
    private static final String TEST_CONNECTOR_NAME = "computedAttributeConnector";

    /** What we end up looking at */
    protected static final String OUTPUT_ATTRIBUTE_NAME = "outputAttribute";

    /** Value calculated using V2 version. DO NOT CHANGE WITHOUT TESTING AGAINST 2.0 */
    protected static final String RESULT = "Vl6z6K70iLc4AuBoNeb59Dj1rGw=";

    protected static final byte salt[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    protected static final byte smallSalt[] = {0, 1, 2};

    private static void testInit(AbstractComputedIDDataConnector connector, String failMessage) {
        try {
            connector.initialize();
            Assert.fail(failMessage);
        } catch (ComponentInitializationException e) {
            // OK
        }
    }

    @Test public void dataConnector() throws ComponentInitializationException, ResolutionException {
        ComputedIDDataConnector connector = new ComputedIDDataConnector();

        connector.setId(TEST_CONNECTOR_NAME);
        connector.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency(
                TestSources.STATIC_ATTRIBUTE_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR)));
        testInit(connector, "No salt");
        connector.setSalt(salt);
        testInit(connector, "No source attr");
        connector.setSourceAttributeId(TestSources.STATIC_ATTRIBUTE_NAME);
        connector.setGeneratedAttributeId(TEST_ATTRIBUTE_NAME);
        connector.initialize();

        SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(OUTPUT_ATTRIBUTE_NAME);
        simple.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency(TEST_CONNECTOR_NAME,
                TEST_ATTRIBUTE_NAME)));

        Set<AttributeDefinition> set = new HashSet<AttributeDefinition>(2);
        set.add(simple);
        set.add(TestSources.populatedStaticAttribute(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR, 1));

        final AttributeResolverImpl resolver =
                new AttributeResolverImpl("atresolver", set, Collections.singleton((DataConnector) connector), null);

        simple.initialize();
        resolver.initialize();
        connector.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(null, null, TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected - two scoped attributes
        Set<IdPAttributeValue<?>> resultValues = context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME).getValues();
        Assert.assertEquals(resultValues.size(), 1);
        Assert.assertEquals(((StringAttributeValue) resultValues.iterator().next()).getValue(), RESULT);
    }

    @Test public void getters() throws ComponentInitializationException {
        ComputedIDDataConnector connector = new ComputedIDDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);
        connector.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency(
                TestSources.STATIC_ATTRIBUTE_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR)));
        connector.setSalt(smallSalt);
        connector.setSourceAttributeId(TestSources.STATIC_ATTRIBUTE_NAME);
        connector.setGeneratedAttributeId(TEST_ATTRIBUTE_NAME);
        Assert.assertEquals(connector.getSalt(), smallSalt);
        testInit(connector, "Small salt");
        connector.setSalt(salt);
        connector.initialize();

        try {
            connector.setSalt(smallSalt);
            Assert.fail("setting after init");
        } catch (UnmodifiableComponentException e) {
            // OK'
        }

        Assert.assertEquals(connector.getSalt(), salt);
    }

    private AttributeResolver constructResolver(int values) throws ComponentInitializationException {
        ComputedIDDataConnector connector = new ComputedIDDataConnector();

        return constructResolver(connector, values);
    }

    protected static AttributeResolver constructResolver(AbstractComputedIDDataConnector connector, int values)
            throws ComponentInitializationException {
        connector.setId(TEST_CONNECTOR_NAME);
        connector.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency(
                TestSources.STATIC_ATTRIBUTE_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR)));
        testInit(connector, "No source attr");
        connector.setSourceAttributeId(TestSources.STATIC_ATTRIBUTE_NAME);
        connector.setSalt(salt);

        SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(OUTPUT_ATTRIBUTE_NAME);
        simple.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency(TEST_CONNECTOR_NAME,
                TEST_CONNECTOR_NAME)));
        simple.initialize();

        Set<AttributeDefinition> set = new HashSet<AttributeDefinition>(2);
        set.add(simple);
        set.add(TestSources.populatedStaticAttribute(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR, values));

        return new AttributeResolverImpl("atresolver", set, Collections.singleton((DataConnector) connector), null);
    }

    private AttributeResolver constructResolverWithNonString(String dependantOn)
            throws ComponentInitializationException {
        return constructResolverWithNonString(new ComputedIDDataConnector(), dependantOn);
    }

    protected static AttributeResolver constructResolverWithNonString(AbstractComputedIDDataConnector connector,
            String dependantOn) throws ComponentInitializationException {
        connector.setId(TEST_CONNECTOR_NAME);
        connector.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency(dependantOn, null)));
        connector.setSalt(salt);
        connector.setSourceAttributeId(dependantOn);

        SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(OUTPUT_ATTRIBUTE_NAME);
        simple.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency(TEST_CONNECTOR_NAME,
                TEST_CONNECTOR_NAME)));
        simple.initialize();
        Set<AttributeDefinition> set = new HashSet<AttributeDefinition>(3);
        set.add(simple);
        set.add(TestSources.populatedStaticAttribute(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR, 1));
        set.add(TestSources.nonStringAttributeDefiniton(dependantOn));

        return new AttributeResolverImpl("atresolver", set, Collections.singleton((DataConnector) connector), null);
    }

    protected static AbstractComputedIDDataConnector connectorFromResolver(AttributeResolver resolver) {
        return (AbstractComputedIDDataConnector) resolver.getDataConnectors().get(TEST_CONNECTOR_NAME);
    }

    //TODO: fix assertion on line 218, see IDP-357
    @Test(enabled = false) public void altDataConnector() throws ComponentInitializationException, ResolutionException {
        AttributeResolver resolver = constructResolver(1);
        connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        AttributeResolutionContext context = TestSources.createResolutionContext(null, null, TestSources.SP_ENTITY_ID);;
        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected
        Set<IdPAttributeValue<?>> resultValues = context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME).getValues();
        Assert.assertEquals(resultValues.size(), 1);
        Assert.assertEquals(((StringAttributeValue) resultValues.iterator().next()).getValue(), RESULT);

        //
        // now do it again with more values
        //
        resolver = constructResolver(3);
        connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        context = TestSources.createResolutionContext(null, null, TestSources.SP_ENTITY_ID);;
        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected - two scoped attributes
        resultValues = context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME).getValues();
        Assert.assertEquals(resultValues.size(), 1);
        Assert.assertEquals(((StringAttributeValue) resultValues.iterator().next()).getValue(), RESULT);

        //
        // And again with different values
        //
        resolver = constructResolver(1);

        connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        context = TestSources.createResolutionContext(null, null, "foo");
        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected - two scoped attributes
        resultValues = context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME).getValues();
        Assert.assertEquals(resultValues.size(), 1);
        Assert.assertNotEquals(((StringAttributeValue) resultValues.iterator().next()).getValue(), RESULT);

    }

    @Test public void attributeFails() throws ComponentInitializationException, ResolutionException {
        AttributeResolver resolver = constructResolver(3);

        connectorFromResolver(resolver).setSourceAttributeId(TestSources.STATIC_ATTRIBUTE_NAME + "1");
        connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        AttributeResolutionContext context = TestSources.createResolutionContext(null, null, TestSources.SP_ENTITY_ID);;
        resolver.resolveAttributes(context);
        Assert.assertNull(context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME));

        resolver = constructResolver(0);
        connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);


        context = TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);
        Assert.assertNull(context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME));

        resolver = constructResolver(1);

        connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        resolver.resolveAttributes(TestSources.createResolutionContext(null, null, null));
        Assert.assertNull(context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME));

        resolver = constructResolverWithNonString("nonString");
        connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        context = TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);
        Assert.assertNull(context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME));

    }
}
