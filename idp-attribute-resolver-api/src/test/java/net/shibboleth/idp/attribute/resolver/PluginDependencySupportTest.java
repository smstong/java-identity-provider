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

package net.shibboleth.idp.attribute.resolver;

import java.util.Map;
import java.util.Set;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

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

/**
 *
 */
public class PluginDependencySupportTest {

    @Test public void getMergedAttributeValueWithAttributeDefinitionDependency() {
        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildAttributeDefinition(
                        ResolverTestSupport.EPA_ATTRIB_ID, ResolverTestSupport.EPA1_VALUES));
        final AttributeResolverWorkContext workContext =
                resolutionContext.getSubcontext(AttributeResolverWorkContext.class, false);

        final Set<IdPAttributeValue<?>> result =
                PluginDependencySupport.getMergedAttributeValues(workContext,
                        Lists.newArrayList(new ResolverPluginDependency(ResolverTestSupport.EPA_ATTRIB_ID)));

        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(new StringAttributeValue(ResolverTestSupport.EPA1_VALUES[0])));
        Assert.assertTrue(result.contains(new StringAttributeValue(ResolverTestSupport.EPA1_VALUES[1])));
    }

    @Test public void getMergedAttributeValuesWithDataConnectorDependency() {
        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1",
                        ResolverTestSupport.buildAttribute(ResolverTestSupport.EPE_ATTRIB_ID,
                                ResolverTestSupport.EPE1_VALUES), ResolverTestSupport.buildAttribute(
                                ResolverTestSupport.EPA_ATTRIB_ID, ResolverTestSupport.EPA1_VALUES)));
        final AttributeResolverWorkContext workContext =
                resolutionContext.getSubcontext(AttributeResolverWorkContext.class, false);

        final ResolverPluginDependency depend = new ResolverPluginDependency("connector1");
        depend.setDependencyAttributeId(ResolverTestSupport.EPE_ATTRIB_ID);
        final Set<IdPAttributeValue<?>> result =
                PluginDependencySupport.getMergedAttributeValues(workContext, Lists.newArrayList(depend));

        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(new StringAttributeValue(ResolverTestSupport.EPE1_VALUES[0])));
        Assert.assertTrue(result.contains(new StringAttributeValue(ResolverTestSupport.EPE1_VALUES[1])));

    }

    @Test public void getMergedAttributeValueWithMultipleDependencies() {
        final MockStaticDataConnector connector1 =
                ResolverTestSupport.buildDataConnector("connector1", ResolverTestSupport.buildAttribute(
                        ResolverTestSupport.EPE_ATTRIB_ID, ResolverTestSupport.EPE1_VALUES), ResolverTestSupport
                        .buildAttribute(ResolverTestSupport.EPA_ATTRIB_ID, ResolverTestSupport.EPA2_VALUES));

        final MockStaticAttributeDefinition definition1 =
                ResolverTestSupport.buildAttributeDefinition(ResolverTestSupport.EPA_ATTRIB_ID,
                        ResolverTestSupport.EPA1_VALUES);

        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(connector1, definition1);
        final AttributeResolverWorkContext workContext =
                resolutionContext.getSubcontext(AttributeResolverWorkContext.class, false);

        final ResolverPluginDependency depend = new ResolverPluginDependency("connector1");
        depend.setDependencyAttributeId(ResolverTestSupport.EPA_ATTRIB_ID);
        final Set<IdPAttributeValue<?>> result =
                PluginDependencySupport.getMergedAttributeValues(workContext,
                        Lists.newArrayList(depend, new ResolverPluginDependency(ResolverTestSupport.EPA_ATTRIB_ID)));

        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 3);
        Assert.assertTrue(result.contains(new StringAttributeValue(ResolverTestSupport.EPA1_VALUES[0])));
        Assert.assertTrue(result.contains(new StringAttributeValue(ResolverTestSupport.EPA1_VALUES[1])));
        Assert.assertTrue(result.contains(new StringAttributeValue(ResolverTestSupport.EPA2_VALUES[1])));

    }

    @Test public void getAllAttributeValues() {
        final MockStaticDataConnector connector1 =
                ResolverTestSupport.buildDataConnector("connector1", ResolverTestSupport.buildAttribute(
                        ResolverTestSupport.EPE_ATTRIB_ID, ResolverTestSupport.EPE1_VALUES), ResolverTestSupport
                        .buildAttribute(ResolverTestSupport.EPA_ATTRIB_ID, ResolverTestSupport.EPA2_VALUES));

        final MockStaticAttributeDefinition definition1 =
                ResolverTestSupport.buildAttributeDefinition(ResolverTestSupport.EPA_ATTRIB_ID,
                        ResolverTestSupport.EPA1_VALUES);

        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(connector1, definition1);
        final AttributeResolverWorkContext workContext =
                resolutionContext.getSubcontext(AttributeResolverWorkContext.class, false);

        final ResolverPluginDependency depend = new ResolverPluginDependency("connector1");
        depend.setDependencyAttributeId(ResolverTestSupport.EPA_ATTRIB_ID);
        final Map<String, Set<IdPAttributeValue<?>>> result =
                PluginDependencySupport.getAllAttributeValues(workContext,
                        Lists.newArrayList(depend, new ResolverPluginDependency(ResolverTestSupport.EPA_ATTRIB_ID)));

        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 2);

        Set<IdPAttributeValue<?>> values = result.get(ResolverTestSupport.EPE_ATTRIB_ID);
        Assert.assertNotNull(values);
        Assert.assertEquals(values.size(), 2);
        Assert.assertTrue(values.contains(new StringAttributeValue(ResolverTestSupport.EPE1_VALUES[0])));
        Assert.assertTrue(values.contains(new StringAttributeValue(ResolverTestSupport.EPE1_VALUES[1])));

        values = result.get(ResolverTestSupport.EPA_ATTRIB_ID);
        Assert.assertNotNull(values);
        Assert.assertEquals(values.size(), 3);
        Assert.assertTrue(values.contains(new StringAttributeValue(ResolverTestSupport.EPA1_VALUES[0])));
        Assert.assertTrue(values.contains(new StringAttributeValue(ResolverTestSupport.EPA1_VALUES[1])));
        Assert.assertTrue(values.contains(new StringAttributeValue(ResolverTestSupport.EPA2_VALUES[1])));
    }
}