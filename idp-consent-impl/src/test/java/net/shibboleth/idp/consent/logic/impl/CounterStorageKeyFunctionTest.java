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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;

import net.shibboleth.idp.consent.flow.impl.ConsentFlowDescriptor;
import net.shibboleth.idp.consent.flow.storage.impl.UpdateCounter;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.component.UnmodifiableComponentException;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for {@link CounterStorageKeyFunction}. */
@SuppressWarnings("javadoc")
public class CounterStorageKeyFunctionTest {

    private RequestContext src;

    private ProfileRequestContext prc;

    private ProfileInterceptorContext pic;

    private ConsentFlowDescriptor descriptor;

    private MemoryStorageService storageService;

    private List<String> keys;

    private Pair<ProfileRequestContext, List<String>> input;

    private CounterStorageKeyFunction f;
    
    private Object nullObj;

    /**
     * Create counter storage records.
     * 
     * @param key storage key
     * @param iterations number of storage record versions
     * @throws IOException if a storage service error occurs
     * @throws InterruptedException if thread error occurs while sleeping
     */
    @SuppressWarnings("null")
    protected void createCounter(@Nonnull final String key, final int iterations) throws IOException,
            InterruptedException {

        final String counterKey = key + ":" + UpdateCounter.COUNTER_KEY;

        storageService.create("mockFlow", counterKey, Long.toString(System.currentTimeMillis()), null);
        for (int i = 1; i < iterations; i++) {
            storageService.update("mockFlow", counterKey, Long.toString(System.currentTimeMillis()), null);
            Thread.sleep(1);
        }
    }

    @BeforeMethod public void setUp() throws Exception {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        final SpringRequestContext springRequestContext = new SpringRequestContext();
        springRequestContext.setRequestContext(src);
        prc.addSubcontext(springRequestContext);

        final MemoryStorageService service = storageService = new MemoryStorageService();
        service.setId("test");
        service.initialize();

        descriptor = new ConsentFlowDescriptor();
        descriptor.setId("test");

        pic = new ProfileInterceptorContext();
        pic.setAttemptedFlow(descriptor);
        final ProfileInterceptorFlowDescriptor flow = pic.getAttemptedFlow();
        assert flow != null;
        flow.setStorageService(service);
        assert pic!=null;
        prc.addSubcontext(pic);

        keys = Arrays.asList("key1", "key2", "key3", "key4");

        input = new Pair<>(prc, keys);

        f = new CounterStorageKeyFunction();
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Test(expectedExceptions = UnmodifiableComponentException.class)
    public void testUnmodifiableInterceptorContextStrategy() throws Exception {
        f.initialize();
        f.setInterceptorContextLookupStrategy((Function<ProfileRequestContext, ProfileInterceptorContext>) nullObj);
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Test(expectedExceptions = UnmodifiableComponentException.class)
    public void testUnmodifiableStorageContextStrategy() throws Exception {
        f.initialize();
        f.setStorageContextLookupStrategy((Function<ProfileRequestContext, String>) nullObj);
    }

    @Test public void testNullPairInput() throws Exception {
        f.initialize();

        final List<String> sortedKeys = f.apply(null);

        Assert.assertNull(sortedKeys);
    }

    @Test public void testNullProfileRequestContextInput() throws Exception {
        f.initialize();

        final List<String> sortedKeys = f.apply(new Pair<>(null, keys));

        Assert.assertNull(sortedKeys);
    }

    @Test public void testNullKeysInput() throws Exception {
        f.initialize();

        final List<String> sortedKeys = f.apply(new Pair<>(prc, null));

        Assert.assertNull(sortedKeys);
    }

    @Test public void testNoProfileInterceptorContext() throws Exception {
        prc.removeSubcontext(ProfileInterceptorContext.class);

        f.initialize();

        final List<String> sortedKeys = f.apply(input);

        Assert.assertNotNull(sortedKeys);

        // insertion order
        Assert.assertEquals(keys, Arrays.asList("key1", "key2", "key3", "key4"));
    }

    @Test public void testNoFlowDescriptor() throws Exception {
        pic = new ProfileInterceptorContext();
        prc.addSubcontext(pic, true);

        f.initialize();

        final List<String> sortedKeys = f.apply(input);

        Assert.assertNotNull(sortedKeys);

        // insertion order
        Assert.assertEquals(keys, Arrays.asList("key1", "key2", "key3", "key4"));
    }

    @Test public void testNoStorageService() throws Exception {
        pic = new ProfileInterceptorContext();
        pic.setAttemptedFlow(descriptor);
        assert pic != null;
        prc.addSubcontext(pic, true);

        f.initialize();

        final List<String> sortedKeys = f.apply(input);

        Assert.assertNotNull(sortedKeys);

        // insertion order
        Assert.assertEquals(keys, Arrays.asList("key1", "key2", "key3", "key4"));
    }

    @Test public void testNoStorageContext() throws Exception {
        prc.removeSubcontext(SpringRequestContext.class);

        f.initialize();

        final List<String> sortedKeys = f.apply(input);

        Assert.assertNotNull(sortedKeys);

        // insertion order
        Assert.assertEquals(keys, Arrays.asList("key1", "key2", "key3", "key4"));
    }

    @Test public void testNoCounters() throws Exception {
        f.initialize();

        final List<String> sortedKeys = f.apply(input);

        Assert.assertNotNull(sortedKeys);

        // insertion order
        Assert.assertEquals(keys, Arrays.asList("key1", "key2", "key3", "key4"));
    }

    @Test public void testSameOrderCounters() throws Exception {
        createCounter("key1", 1);
        createCounter("key2", 1);
        createCounter("key3", 1);
        createCounter("key4", 1);

        f.initialize();

        final List<String> sortedKeys = f.apply(input);

        Assert.assertNotNull(sortedKeys);

        // insertion order
        Assert.assertEquals(keys, Arrays.asList("key1", "key2", "key3", "key4"));
    }

    @Test public void testDifferentOrderCounters() throws Exception {
        createCounter("key1", 4);
        createCounter("key2", 2);
        createCounter("key3", 1);
        createCounter("key4", 3);

        f.initialize();

        final List<String> sortedKeys = f.apply(input);

        Assert.assertNotNull(sortedKeys);

        // insertion order
        Assert.assertEquals(keys, Arrays.asList("key3", "key2", "key4", "key1"));
    }

    @Test public void testMissingCounters() throws Exception {
        createCounter("key1", 4);

        createCounter("key3", 1);
        createCounter("key4", 3);

        f.initialize();

        final List<String> sortedKeys = f.apply(input);

        Assert.assertNotNull(sortedKeys);

        // insertion order
        Assert.assertEquals(keys, Arrays.asList("key2", "key3", "key4", "key1"));
    }
}
