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

package net.shibboleth.idp.consent.audit.impl;

import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.consent.impl.ConsentTestingSupport;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.RequestContext;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for audit extractor tests.
 */
public class AbstractConsentAuditExtractorTest {

    protected RequestContext src;

    protected ProfileRequestContext prc;

    /**
     * Set up for tests.
     * 
     * @throws Exception
     */
    @BeforeMethod public void setUp() throws Exception {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        final ConsentContext consentCtx = new ConsentContext();
        consentCtx.getCurrentConsents().putAll(ConsentTestingSupport.newConsentMap());
        prc.addSubcontext(consentCtx, true);
    }
}
