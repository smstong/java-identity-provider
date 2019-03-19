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

package net.shibboleth.idp.attribute.resolver.spring.dc;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.ext.spring.config.DurationToLongConverter;
import net.shibboleth.ext.spring.config.StringToDurationConverter;
import net.shibboleth.ext.spring.config.StringToIPRangeConverter;
import net.shibboleth.ext.spring.config.StringToResourceConverter;
import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.idp.attribute.impl.ComputedPairwiseIdStore;
import net.shibboleth.idp.attribute.impl.JDBCPairwiseIdStore;
import net.shibboleth.idp.attribute.resolver.ResolverAttributeDefinitionDependency;
import net.shibboleth.idp.attribute.resolver.dc.impl.PairwiseIdDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.StoredIdDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.rdbms.RDBMSDataConnectorParserTest;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Test for {@link StoredIdDataConnectorParser}.
 */
public class StoredIdDataConnectorParserTest extends BaseAttributeDefinitionParserTest {
    
    private void testIt(final PairwiseIdDataConnector connector) throws ComponentInitializationException {
        
        final JDBCPairwiseIdStore store = (JDBCPairwiseIdStore) connector.getPairwiseIdStore();
        
        Assert.assertEquals(connector.getId(), "stored");
        Assert.assertEquals(connector.getGeneratedAttributeId(), "jenny");
        Assert.assertEquals(store.getTransactionRetries(), 5);
        Assert.assertEquals(store.getQueryTimeout(), Duration.ofSeconds(5));
        Assert.assertEquals(store.getVerifyDatabase(), false);
        Assert.assertTrue(Arrays.equals(store.getRetryableErrors().toArray(), new String[]{"25000", "25001"}));
        
        connector.initialize();
    }
    
    @Test public void withSalt() throws ComponentInitializationException {
        final PairwiseIdDataConnector connector = getDataConnector("resolver/stored.xml", PairwiseIdDataConnector.class);
        final JDBCPairwiseIdStore store = (JDBCPairwiseIdStore) connector.getPairwiseIdStore();
        final ComputedPairwiseIdStore store2 = (ComputedPairwiseIdStore) store.getInitialValueStore();

        final ResolverAttributeDefinitionDependency attrib = connector.getAttributeDependencies().iterator().next();
        Assert.assertEquals(attrib.getDependencyPluginId(), "theSourceRemainsTheSame");
        Assert.assertEquals(store2.getSalt(), "abcdefghijklmnopqrst".getBytes());
        testIt(connector);
    }

    protected PairwiseIdDataConnector getStoredDataConnector(final String... beanDefinitions) throws IOException {
        final GenericApplicationContext context = new GenericApplicationContext();
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + RDBMSDataConnectorParserTest.class);

        final ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        service.setConverters(new HashSet<>(Arrays.asList(
                new DurationToLongConverter(),
                new StringToIPRangeConverter(),
                new StringToResourceConverter(),
                new StringToDurationConverter())));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());

        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidating(true);
        beanDefinitionReader.loadBeanDefinitions(beanDefinitions);
        context.refresh();

        return (PairwiseIdDataConnector) context.getBean(PairwiseIdDataConnector.class);
    }

    @Test public void beanManaged() throws ComponentInitializationException, IOException {
        final PairwiseIdDataConnector connector = getStoredDataConnector(DATACONNECTOR_FILE_PATH + "resolver/storedBeanManaged.xml", 
                DATACONNECTOR_FILE_PATH + "rdbms/rdbms-attribute-resolver-spring-context.xml");
        final JDBCPairwiseIdStore store = (JDBCPairwiseIdStore) connector.getPairwiseIdStore();
        final ComputedPairwiseIdStore store2 = (ComputedPairwiseIdStore) store.getInitialValueStore();
        
        final ResolverAttributeDefinitionDependency attrib = connector.getAttributeDependencies().iterator().next();
        Assert.assertEquals(attrib.getDependencyPluginId(), "theSourceRemainsTheSame");
        Assert.assertEquals(store2.getSalt(), "abcdefghijklmnopqrst".getBytes());
        testIt(connector);
    }



    @Test public void withOutSalt() throws ComponentInitializationException {
        final PairwiseIdDataConnector connector = getDataConnector("resolver/storedNoSalt.xml", PairwiseIdDataConnector.class);
        final ResolverAttributeDefinitionDependency attrib = connector.getAttributeDependencies().iterator().next();
        Assert.assertEquals(attrib.getDependencyPluginId(), "theSourceRemainsTheSame");
        testIt(connector);
    }
}