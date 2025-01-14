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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextDeclRef;
import org.opensaml.saml.saml2.core.AuthnStatement;

import net.shibboleth.shared.logic.Constraint;

/**
 * {@link Function} that returns the first AuthenticationMethod, AuthnContextCLassRef,
 * or AuthnContextDeclRef from an assertions in a response.
 */
public class AuthnContextAuditExtractor implements Function<ProfileRequestContext,String> {

    /** Lookup strategy for message to read from. */
    @Nonnull private final Function<ProfileRequestContext,SAMLObject> responseLookupStrategy;
    
    /**
     * Constructor.
     *
     * @param strategy lookup strategy for message
     */
    public AuthnContextAuditExtractor(@Nonnull final Function<ProfileRequestContext,SAMLObject> strategy) {
        responseLookupStrategy = Constraint.isNotNull(strategy, "Response lookup strategy cannot be null");
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final ProfileRequestContext input) {
        SAMLObject response = responseLookupStrategy.apply(input);
        if (response != null) {
            
            // Step down into ArtifactResponses.
            if (response instanceof ArtifactResponse) {
                response = ((ArtifactResponse) response).getMessage();
            }
            
            if (response instanceof org.opensaml.saml.saml2.core.Response) {
                
                for (final org.opensaml.saml.saml2.core.Assertion assertion
                        : ((org.opensaml.saml.saml2.core.Response) response).getAssertions()) {
                    for (final AuthnStatement statement : assertion.getAuthnStatements()) {
                        final AuthnContext ac = statement.getAuthnContext();
                        if (ac != null) {
                            final AuthnContextClassRef acClass = ac.getAuthnContextClassRef();
                            if (acClass != null && acClass.getURI() != null) {
                                return acClass.getURI();
                            }
                            
                            final AuthnContextDeclRef acDecl = ac.getAuthnContextDeclRef();
                            if (acDecl != null && acDecl.getURI() != null) {
                                return acDecl.getURI();
                            }
                        }
                    }
                }
                
            } else if (response instanceof org.opensaml.saml.saml1.core.Response) {
                
                for (final org.opensaml.saml.saml1.core.Assertion assertion
                        : ((org.opensaml.saml.saml1.core.Response) response).getAssertions()) {
                    for (final AuthenticationStatement statement : assertion.getAuthenticationStatements()) {
                        if (statement.getAuthenticationMethod() != null) {
                            return statement.getAuthenticationMethod();
                        }
                    }
                }

            }
        }
        
        return null;
    }
// Checkstyle: CyclomaticComplexity ON
    
}