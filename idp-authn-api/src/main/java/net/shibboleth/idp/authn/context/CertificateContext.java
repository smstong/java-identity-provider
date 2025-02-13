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

package net.shibboleth.idp.authn.context;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.logic.Constraint;

import org.opensaml.messaging.context.BaseContext;

/**
 * Context that carries a {@link Certificate} to be validated.
 * 
 * @parent {@link AuthenticationContext}
 * @added After extraction of a certificate during authentication
 */
public final class CertificateContext extends BaseContext {

    /** The certificate to be validated. */
    @Nullable private Certificate certificate;
    
    /** Additional certificates as input to validation. */
    @Nonnull private Collection<Certificate> intermediates;

    /** Constructor. */
    public CertificateContext() {
        intermediates = new ArrayList<>();
    }
    
    /**
     * Get the certificate to be validated.
     * 
     * @return the certificate to be validated
     */
    @Nullable public Certificate getCertificate() {
        return certificate;
    }

    /**
     * Set the certificate to be validated.
     * 
     * @param cert certificate to be validated
     * 
     * @return this context
     */
    @Nonnull public CertificateContext setCertificate(@Nullable final Certificate cert) {
        certificate = cert;
        return this;
    }
    
    /**
     * Get any additional certificates accompanying the end-entity certificate.
     * 
     * @return any additional certificates
     */
    @Nonnull @Live public Collection<Certificate> getIntermediates() {
        return intermediates;
    }

    /**
     * Set the additional certificates accompanying the end-entity certificate.
     * 
     * @param certs additional certificates
     * 
     * @return this context
     */
    @Nonnull public CertificateContext setIntermediates(@Nonnull final Collection<Certificate> certs) {
        Constraint.isNotNull(certs, "Intermediate certificate collection cannot be null");
        
        intermediates.clear();
        intermediates.addAll(List.copyOf(certs));
        
        return this;
    }
    
}