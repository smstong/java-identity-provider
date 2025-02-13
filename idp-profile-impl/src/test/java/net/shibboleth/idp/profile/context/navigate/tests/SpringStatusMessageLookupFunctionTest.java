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

package net.shibboleth.idp.profile.context.navigate.tests;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.idp.profile.context.navigate.SpringStatusMessageLookupFunction;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.component.ComponentInitializationException;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link SpringStatusMessageLookupFunction} unit test. */
@SuppressWarnings("javadoc")
public class SpringStatusMessageLookupFunctionTest {

    private MockRequestContext springRequestContext;
    
    private ProfileRequestContext prc;
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        springRequestContext = (MockRequestContext) new RequestContextBuilder().buildRequestContext();
        prc = (ProfileRequestContext) springRequestContext.getConversationScope().get(ProfileRequestContext.BINDING_KEY);
        prc.ensureSubcontext(SpringRequestContext.class).setRequestContext(springRequestContext);
    }

    @Test public void testMappedMessage() {
        
        final SpringStatusMessageLookupFunction fn = new SpringStatusMessageLookupFunction();
        
        fn.setMessageSource(new MockMessageSource());

        String message = fn.apply(prc);
        Assert.assertNull(message);

        springRequestContext.setCurrentEvent(new Event(this, "Mappable"));
        
        message = fn.apply(prc);
        Assert.assertEquals(message, "Mapped");
    }
    
    private class MockMessageSource implements MessageSource {

        /** {@inheritDoc} */
        public String getMessage(@Nonnull String code, @Nullable Object[] args, @Nullable String defaultMessage, @Nonnull Locale locale) {
            if (code.equals("Mappable")) {
                return "Mapped";
            }
            return defaultMessage;
        }

        /** {@inheritDoc} */
        public @Nonnull String getMessage(@Nonnull String code, @Nullable Object[] args, @Nonnull Locale locale) throws NoSuchMessageException {
            if (code.equals("Mappable")) {
                return "Mapped";
            }
            throw new NoSuchMessageException("No such message");
        }

        /** {@inheritDoc} */
        public @Nonnull String getMessage(@Nonnull MessageSourceResolvable resolvable, @Nonnull Locale locale) throws NoSuchMessageException {
            final String[] codes = resolvable.getCodes();
            assert codes != null && codes.length >=1;
            if (codes[0].equals("Mappable")) {
                return "Mapped";
            }
            throw new NoSuchMessageException("No such message");
        }
        
    }
    
}