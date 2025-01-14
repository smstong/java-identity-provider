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

package net.shibboleth.idp.saml.audit.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.SessionIndex;

import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;

/** {@link Function} that returns SessionIndex values from assertions in a response or a logout request. */
public class SessionIndexAuditExtractor implements Function<ProfileRequestContext,Collection<String>> {

    /** Lookup strategy for message to read from. */
    @Nonnull private final Function<ProfileRequestContext,SAMLObject> messageLookupStrategy;
    
    /**
     * Constructor.
     *
     * @param strategy lookup strategy for message
     */
    public SessionIndexAuditExtractor(@Nonnull final Function<ProfileRequestContext,SAMLObject> strategy) {
        messageLookupStrategy = Constraint.isNotNull(strategy, "Message lookup strategy cannot be null");
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Nullable @Unmodifiable @NotLive public Collection<String> apply(@Nullable final ProfileRequestContext input) {
        
        SAMLObject message = messageLookupStrategy.apply(input);
        if (message != null) {
            
            final Collection<String> indexes = new ArrayList<>(1);
            
            // Step down into ArtifactResponses.
            if (message instanceof ArtifactResponse) {
                message = ((ArtifactResponse) message).getMessage();
            }
            
            if (message instanceof Response) {
                for (final Assertion assertion : ((Response) message).getAssertions()) {
                    for (final AuthnStatement statement : assertion.getAuthnStatements()) {
                        if (statement.getSessionIndex() != null) {
                            indexes.add(statement.getSessionIndex());
                        }
                    }
                }
            } else if (message instanceof LogoutRequest) {
                for (final SessionIndex index : ((LogoutRequest) message).getSessionIndexes()) {
                    if (index != null && index.getValue() != null) {
                        indexes.add(index.getValue());
                    }
                }
            }
            
            return indexes;
        }
        
        return CollectionSupport.emptyList();
    }
// Checkstyle: CyclomaticComplexity ON
    
}