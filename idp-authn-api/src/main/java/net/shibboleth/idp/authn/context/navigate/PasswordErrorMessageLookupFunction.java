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

package net.shibboleth.idp.authn.context.navigate;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.context.support.MessageSourceAccessor;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.AuthenticationErrorContext;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * A function that examines the state of a request and produces an appropriate error message for
 * the Password login flow.
 * 
 * <p>NOTE: The result of this function is <strong>NOT</strong> HTML-encoded in any way and must
 * be encoded for safety if used.</p>
 * 
 * <p>This implements the pre-existing default behavior in Velocity for determining an error to
 * display.</p>
 * 
 * @since 5.1.0
 */
public class PasswordErrorMessageLookupFunction extends ApplicationObjectSupport
        implements ContextDataLookupFunction<ProfileRequestContext,String> {
    
    /** Message ID to use for generic, unclassified errors or exceptions. */
    private String genericMessageID;
    
    /**
     * Sets whether non-message-based error messages should be exposed or turned into a more
     * generic value.
     * 
     * @param id message ID
     */
    public void setGenericMessageID(@Nullable final String id) {
        genericMessageID = StringSupport.trimOrNull(id);
    }
    
    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final ProfileRequestContext input) {
        
        final MessageSourceAccessor messageSource = getMessageSourceAccessor();
        if (messageSource == null) {
            return null;
        }
        
        final AuthenticationContext authCtx = input != null ? input.getSubcontext(AuthenticationContext.class) : null;
        final AuthenticationErrorContext errorCtx =
                authCtx != null ? authCtx.getSubcontext(AuthenticationErrorContext.class) : null;

        if (errorCtx == null) {
            return null;
        }
        
        final Collection<String> classifiedErrors = errorCtx.getClassifiedErrors(); 
        if (!classifiedErrors.isEmpty() && !classifiedErrors.contains(AuthnEventIds.AUTHN_EXCEPTION)) {
            return getClassifiedMessage(messageSource, classifiedErrors.iterator().next());
        } else if (!errorCtx.getExceptions().isEmpty()) {
            return getExceptionMessage(messageSource, errorCtx.getExceptions().get(0));
        }
        
        return null;
    }
    
    /**
     * Get classified message.
     * 
     * @param messageSource Spring message source
     * @param classifiedError classified error event
     * 
     * @return mapped message, or null
     */
    @Nullable private String getClassifiedMessage(@Nonnull final MessageSourceAccessor messageSource,
            @Nonnull final String classifiedError) {
        
        if (!AuthnEventIds.RESELECT_FLOW.equals(classifiedError)) {
            final String eventKey = messageSource.getMessage(classifiedError,
                    genericMessageID != null ? genericMessageID : "authn");
            if (eventKey != null) {
                return messageSource.getMessage(eventKey + ".message", "Login Failure: " + classifiedError);
            }
        }
        
        return null;
    }
    
    /**
     * Get generic error message or exception message. 
     * 
     * @param messageSource Spring message source
     * @param e exception
     * 
     * @return the exception message or the generic message as appropriate
     */
    @Nullable private String getExceptionMessage(@Nonnull final MessageSourceAccessor messageSource,
            @Nonnull final Exception e) {
        
        if (genericMessageID != null) {
            return messageSource.getMessage(genericMessageID, "Login was not successful.");
        }
        
        if (e.getMessage() != null) {
            return "Login Failure: " + e.getMessage();
        } else {
            return e.toString();
        }
    }

}