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

package net.shibboleth.idp.authn.impl;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.security.SecurityException;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.X509Credential;
import org.slf4j.Logger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.ExternalAuthenticationException;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.CertificateContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.resolver.CriteriaSet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet compatible with the {@link ExternalAuthentication} interface that extracts and validates
 * an X.509 client certificate for user authentication.
 */
public class X509AuthServlet extends HttpServlet {
    
    /** Serial UUID. */
    private static final long serialVersionUID = 7466474175700654990L;
    
    /** Init parameter identifying optional {@link TrustEngine} bean name. */
    @Nonnull @NotEmpty private static final String TRUST_ENGINE_PARAM = "trustEngine";

    /** Init parameter controlling certificate preservation. */
    @Nonnull @NotEmpty private static final String SAVECERT_PARAM = "saveCertificateToCredentialSet";

    /** Parameter/cookie for bypassing prompt page. */
    @Nonnull @NotEmpty private static final String PASSTHROUGH_PARAM = "x509passthrough";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(X509AuthServlet.class);

    /** Trust engine. */
    @Nullable private TrustEngine<? super X509Credential> trustEngine;
    
    /** Whether to save the certificate to the Java Subject's public credentials. */
    private boolean saveCertificateToCredentialSet;

    /** Constructor. */
    public X509AuthServlet() {
        saveCertificateToCredentialSet = true;
    }
    
    /**
     * Set the {@link TrustEngine} to use.
     * 
     * @param tm trust engine to use  
     */
    public void setTrustEngine(@Nullable final TrustEngine<? super X509Credential> tm) {
        trustEngine = tm;
    }

    /**
     * Set whether to save the certificate in the Java Subject's public credentials.
     * 
     * <p>Defaults to true</p>
     * 
     * @param flag flag to set
     * 
     * @since 4.1.0
     */
    public void setSaveCertificateToCredentialSet(final boolean flag) {
        saveCertificateToCredentialSet = flag;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        final ServletContext servletContext = getServletContext();
        assert servletContext != null;
        final WebApplicationContext springContext =
                WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        
        String param = config.getInitParameter(TRUST_ENGINE_PARAM);
        if (param != null) {
            log.debug("Looking up TrustEngine bean: {}", param);
            final Object bean = springContext.getBean(param);
            if (bean instanceof TrustEngine) {
                trustEngine = (TrustEngine<? super X509Credential>) bean;
            } else {
                throw new ServletException("Bean " + param + " was missing, or not a TrustManager");
            }
        }
        
        param = config.getInitParameter(SAVECERT_PARAM);
        if (param != null) {
            setSaveCertificateToCredentialSet(Boolean.valueOf(param));
        }
    }

// Checkstyle: CyclomaticComplexity|MethodLength OFF
    /** {@inheritDoc} */
    @Override
    protected void service(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse)
            throws ServletException, IOException {
        
        assert httpRequest != null && httpResponse != null;
        try {
            final String key = ExternalAuthentication.startExternalAuthentication(httpRequest);
            
            X509Certificate[] certs =
                    (X509Certificate[]) httpRequest.getAttribute("jakarta.servlet.request.X509Certificate");
            if (certs == null || certs.length == 0) {
                // Check for older variant.
                certs = (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
            }
            log.debug("{} X.509 Certificate(s) found in request", certs != null ? certs.length : 0);

            if (certs == null || certs.length == 0) {
                log.error("No X.509 Certificates found in request");
                httpRequest.setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY, AuthnEventIds.NO_CREDENTIALS);
                ExternalAuthentication.finishExternalAuthentication(key, httpRequest, httpResponse);
                return;
            }

            final X509Certificate cert = certs[0];
            log.debug("End-entity X.509 certificate found with subject '{}', issued by '{}'",
                    cert.getSubjectX500Principal().getName(), cert.getIssuerX500Principal().getName());
            
            // Populate the cert chain into a CertificateContext for auditing.
            final ProfileRequestContext prc = ExternalAuthentication.getProfileRequestContext(key, httpRequest);
            final AuthenticationContext authnCtx = prc.getSubcontext(AuthenticationContext.class);
            if (authnCtx != null) {
                final CertificateContext cc = authnCtx.ensureSubcontext(CertificateContext.class);
                cc.setCertificate(cert);
                if (certs.length > 1) {
                    for (int i = 1; i < certs.length; i++) {
                        cc.getIntermediates().add(certs[i]);
                    }
                }
            }

            if (trustEngine != null) {
                try {
                    final BasicX509Credential cred = new BasicX509Credential(cert);
                    cred.setEntityCertificateChain(Arrays.asList(certs));
                    assert trustEngine != null;
                    if (trustEngine.validate(cred, new CriteriaSet())) {
                        log.debug("Trust engine validated X.509 certificate");
                    } else {
                        log.warn("Trust engine failed to validate X.509 certificate");
                        httpRequest.setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY,
                                AuthnEventIds.INVALID_CREDENTIALS);
                        ExternalAuthentication.finishExternalAuthentication(key, httpRequest, httpResponse);
                        return;
                    }
                } catch (final SecurityException e) {
                    log.error("Exception raised by trust engine", e);
                    httpRequest.setAttribute(ExternalAuthentication.AUTHENTICATION_EXCEPTION_KEY, e);
                    ExternalAuthentication.finishExternalAuthentication(key, httpRequest, httpResponse);
                    return;
                }
            }
            
            final String passthrough = httpRequest.getParameter(PASSTHROUGH_PARAM);
            if (passthrough != null && Boolean.parseBoolean(passthrough)) {
                log.debug("Setting UI passthrough cookie");
                final Cookie cookie = new Cookie(PASSTHROUGH_PARAM, "1");
                cookie.setPath(httpRequest.getContextPath());
                cookie.setMaxAge(60 * 60 * 24 * 365);
                cookie.setSecure(true);
                httpResponse.addCookie(cookie);
            }
            
            final Subject subject = new Subject();
            if (saveCertificateToCredentialSet) {
                subject.getPublicCredentials().add(cert);
            }
            subject.getPrincipals().add(cert.getSubjectX500Principal());

            httpRequest.setAttribute(ExternalAuthentication.SUBJECT_KEY, subject);

            final String revokeConsent =
                    httpRequest.getParameter(ExternalAuthentication.REVOKECONSENT_KEY);
            if (revokeConsent != null && ("1".equals(revokeConsent) || "true".equals(revokeConsent))) {
                httpRequest.setAttribute(ExternalAuthentication.REVOKECONSENT_KEY, Boolean.TRUE);
            }

            ExternalAuthentication.finishExternalAuthentication(key, httpRequest, httpResponse);
            
        } catch (final ExternalAuthenticationException e) {
            throw new ServletException("Error processing external authentication request", e);
        }
    }
// Checkstyle: CyclomaticComplexity|MethodLength ON
    
}