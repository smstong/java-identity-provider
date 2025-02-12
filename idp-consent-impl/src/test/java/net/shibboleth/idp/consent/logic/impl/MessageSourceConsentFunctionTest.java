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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.flow.impl.ConsentFlowDescriptor;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.component.UnmodifiableComponentException;
import net.shibboleth.shared.logic.ConstraintViolationException;
import net.shibboleth.shared.logic.FunctionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link MessageSourceConsentFunction} unit test. */
@SuppressWarnings("javadoc")
public class MessageSourceConsentFunctionTest {

    private RequestContext src;

    private ProfileRequestContext prc;

    private MessageSource messageSource;

    private MessageSourceConsentFunction function;
    
    private Object nullObject;

    @BeforeMethod public void setUp() throws Exception {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        messageSource = new MockMessageSource();

        function = new MessageSourceConsentFunction();
        assert messageSource!=null;
        function.setMessageSource(messageSource);
    }

    /**
     * Add a {@link ConsentFlowDescriptor} to the {@link ProfileRequestContext}.
     * 
     * @param compareValues whether consent equality includes comparing consent values
     */
    private void setUpDescriptor(final boolean compareValues) {
        final ConsentFlowDescriptor descriptor = new ConsentFlowDescriptor();
        descriptor.setId("test");
        descriptor.setCompareValues(compareValues);

        final ProfileInterceptorContext pic = new ProfileInterceptorContext();
        pic.setAttemptedFlow(descriptor);
        prc.addSubcontext(pic);
        final ProfileInterceptorContext pic2 =prc.getSubcontext(ProfileInterceptorContext.class);
        assert pic2!=null;
        final ProfileInterceptorFlowDescriptor flow = pic2.getAttemptedFlow();
        assert flow != null;
        Assert.assertTrue(flow  instanceof ConsentFlowDescriptor);

        Assert.assertEquals(((ConsentFlowDescriptor) flow).compareValues(), compareValues);
    }

    @Test public void testNullInput() {
        Assert.assertNull(function.apply(null));
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Test(expectedExceptions = ConstraintViolationException.class) public void testNullIdMessageCode() throws Exception {
        function.setConsentKeyLookupStrategy((Function<ProfileRequestContext, String>) nullObject);
        function.initialize();
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Test(expectedExceptions = ConstraintViolationException.class) public void testNullValueMessageCode()
            throws Exception {
        function.setConsentValueMessageCodeSuffix((String) nullObject);
        function.initialize();
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testEmptyValueMessageCode()
            throws Exception {
        function.setConsentValueMessageCodeSuffix("");
        function.initialize();
    }

    @Test(expectedExceptions = UnmodifiableComponentException.class) public void testInstantiationIdMessageCode()
            throws Exception {
        function.setConsentKeyLookupStrategy(FunctionSupport.constant("consentIdMessageCode"));
        function.initialize();

        function.setConsentKeyLookupStrategy(FunctionSupport.constant("consentIdMessageCode"));
    }

    @Test public void testMessageSourceConsent() throws Exception {

        setUpDescriptor(false);

        function.setConsentKeyLookupStrategy(FunctionSupport.constant("key"));
        function.setLocaleLookupStrategy(e -> new Locale("en"));
        function.initialize();

        final Consent consent = new Consent();
        consent.setId("id");

        final Map<String, Consent> expected = new HashMap<>();
        expected.put(consent.getId(), consent);

        Assert.assertEquals(function.apply(prc), expected);
    }

    @SuppressWarnings("null")
    @Test public void testMessageSourceConsentCompareValues() throws Exception {

        setUpDescriptor(true);

        function.setConsentKeyLookupStrategy(FunctionSupport.constant("key"));
        function.setLocaleLookupStrategy(e -> new Locale("en"));
        function.initialize();

        final Consent consent = new Consent();
        consent.setId("id");
        consent.setValue(function.getHashFunction().apply("value"));

        final Map<String, Consent> expected = new HashMap<>();
        expected.put(consent.getId(), consent);

        Assert.assertEquals(function.apply(prc), expected);
    }

    private class MockMessageSource implements MessageSource {

        /** {@inheritDoc} */
        public String getMessage(@Nonnull String code, @Nullable Object[] args, @Nullable String defaultMessage, @Nonnull Locale locale) {
            if (code.equals("key")) {
                return "id";
            } else if (code.equals("id.text")) {
                return "value";
            } else {
                return defaultMessage;
            }
        }

        /** {@inheritDoc} */
        public @Nonnull String getMessage(@Nonnull String code, @Nullable Object[] args, @Nonnull Locale locale) throws NoSuchMessageException {
            if (code.equals("key")) {
                return "id";
            } else if (code.equals("id.text")) {
                return "value";
            }
            throw new NoSuchMessageException("No such message");
        }

        /** {@inheritDoc} */
        public @Nonnull String getMessage(@Nonnull MessageSourceResolvable resolvable, @Nonnull Locale locale) throws NoSuchMessageException {
            final String[] codes = resolvable.getCodes();
            assert codes != null;
            if (codes[0].equals("key")) {
                return "id";
            } else if (codes[0].equals("id.text")) {
                return "value";
            }
            throw new NoSuchMessageException("No such message");
        }
    }

}