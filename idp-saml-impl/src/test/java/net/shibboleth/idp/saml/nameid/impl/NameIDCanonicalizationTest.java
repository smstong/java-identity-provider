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

package net.shibboleth.idp.saml.nameid.impl;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.saml.authn.principal.NameIDPrincipal;
import net.shibboleth.idp.saml.nameid.NameDecoderException;
import net.shibboleth.idp.saml.nameid.NameIDCanonicalizationFlowDescriptor;
import net.shibboleth.idp.saml.nameid.NameIDDecoder;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.testing.ActionTestingSupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml2.core.NameID;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link NameIDCanonicalization} unit test. */
@SuppressWarnings({"javadoc", "null"})
public class NameIDCanonicalizationTest extends OpenSAMLInitBaseTestCase {

    private ProfileRequestContext prc;

    private NameIDCanonicalization action;

    private SAMLObjectBuilder<NameID> builder;
    
    private NameIDCanonicalizationFlowDescriptor flowDescriptor;
    
    private static final String REQUESTER = "TestRequest";

    private static final String RESPONDER = "TestResp";

    private static final String VALUE_PREFIX = "TestPrefix";

    private static final List<String> formats = Arrays.asList(NameID.KERBEROS, NameID.ENCRYPTED, null);

    @BeforeClass public void initialize() {
        builder = (SAMLObjectBuilder<NameID>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<NameID>ensureBuilder(
                        NameID.DEFAULT_ELEMENT_NAME);
    }

    @BeforeMethod public void setUp() throws Exception {
        prc = new ProfileRequestContext();
        
        flowDescriptor = new NameIDCanonicalizationFlowDescriptor();
        flowDescriptor.setId("C14NDesc");
        flowDescriptor.setFormats(formats);
        flowDescriptor.initialize();
        
        action = new NameIDCanonicalization();
        action.setDecoder(new NameIDDecoder() {
            public String decode(SubjectCanonicalizationContext scc, NameID nameID) throws NameDecoderException {
                if (RESPONDER.equals(scc.getResponderId()) && REQUESTER.equals(scc.getRequesterId())) {
                    return VALUE_PREFIX + nameID.getValue();
                }
                return null;
            }
        });
        action.initialize();
    }

    private void setSubContext(@Nullable Subject subject, @Nullable String responder, @Nullable String requester) {
        final SubjectCanonicalizationContext scc = prc.ensureSubcontext(SubjectCanonicalizationContext.class);
        if (subject != null) {
            scc.setSubject(subject);
        }
        if (requester != null) {
            scc.setRequesterId(requester);
        }
        if (responder != null) {
            scc.setResponderId(responder);
        }
        scc.setAttemptedFlow(flowDescriptor);
    }

    private NameID nameId(String value, String format, String nameQualifier, String nameSPQualifier) {

        final NameID id = builder.buildObject();

        id.setValue(value);
        id.setFormat(format);
        id.setNameQualifier(nameQualifier);
        id.setSPNameQualifier(nameSPQualifier);
        return id;
    }

    private NameID nameId(String value, String format) {
        return nameId(value, format, RESPONDER, REQUESTER);
    }

    @Test public void testFormatCount() {
        Assert.assertEquals(flowDescriptor.getFormats().size(), 2);
    }

    @Test public void testNoContext() {
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT_C14N_CTX);
    }

    @Test public void testNoPrincipal() {
        final Subject subject = new Subject();
        setSubContext(subject, null, null);

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT);
        final SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert scc!=null && scc.getException()!=null;
    }

    @Test public void testMultiPrincipals() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new NameIDPrincipal(nameId("value", NameID.KERBEROS)));
        subject.getPrincipals().add(new NameIDPrincipal(nameId("value2", NameID.X509_SUBJECT)));

        setSubContext(subject, null, null);

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT);
        final SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert scc!=null && scc.getException()!=null;
    }

    @Test public void testWrongFormat() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new NameIDPrincipal(nameId("value", NameID.X509_SUBJECT)));

        setSubContext(subject, RESPONDER, REQUESTER);

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT);
        final SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert scc!=null && scc.getException()!=null;
    }

    @Test public void testWrongRequesterNameID() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new NameIDPrincipal(nameId("value", NameID.KERBEROS)));
        setSubContext(subject, RESPONDER, RESPONDER);

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT);
    }

    @Test public void testWrongResponderNameID() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new NameIDPrincipal(nameId("value", NameID.KERBEROS)));
        setSubContext(subject, REQUESTER, REQUESTER);

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT);
    }

    @Test public void testWrongRequester() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new NameIDPrincipal(nameId("value", NameID.KERBEROS, REQUESTER, REQUESTER)));
        setSubContext(subject, RESPONDER, RESPONDER);

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT);
    }

    @Test public void testWrongResponder() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new NameIDPrincipal(nameId("value", NameID.KERBEROS, REQUESTER, REQUESTER)));
        setSubContext(subject, REQUESTER, REQUESTER);

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT);
    }

    @Test public void testSuccess() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new UsernamePrincipal("foo@osu.edu"));
        subject.getPrincipals().add(new NameIDPrincipal(nameId("works", NameID.KERBEROS)));
        setSubContext(subject, RESPONDER, REQUESTER);

        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        SubjectCanonicalizationContext sc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert sc!=null;
        Assert.assertEquals(sc.getPrincipalName(), VALUE_PREFIX + "works");
    }

}