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

package net.shibboleth.idp.saml.profile;

import javax.annotation.Nonnull;

import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * Constants to use for audit logging fields stored in an {@link net.shibboleth.profile.context.AuditContext}.
 */
public final class SAMLAuditFields {

    /** Service Provider field. */
    @Nonnull @NotEmpty public static final String SERVICE_PROVIDER = "SP";

    /** Identity Provider field. */
    @Nonnull @NotEmpty public static final String IDENTITY_PROVIDER = "IDP";

    /** Protocol field. */
    @Nonnull @NotEmpty public static final String PROTOCOL = "p";

    /** Request binding field. */
    @Nonnull @NotEmpty public static final String REQUEST_BINDING = "b";
    
    /** Response binding field. */
    @Nonnull @NotEmpty public static final String RESPONSE_BINDING = "bb";
    
    /** RelayState binding field. */
    @Nonnull @NotEmpty public static final String RELAY_STATE = "RS";
    
    /** Name identifier field. */
    @Nonnull @NotEmpty public static final String NAMEID = "n";

    /** Name identifier Format field. */
    @Nonnull @NotEmpty public static final String NAMEID_FORMAT = "f";
    
    /** NameID SPNameQualifier field. @since 4.0.0 */
    @Nonnull @NotEmpty public static final String SP_NAME_QUALIFIER = "SPQ";

    /** Name identifier Format field. */
    @Nonnull @NotEmpty public static final String NAMEIDPOLICY_FORMAT = "pf";
    
    /** NameID SPNameQualifier field. @since 4.0.0 */
    @Nonnull @NotEmpty public static final String NAMEIDPOLICY_SP_NAME_QUALIFIER = "PSPQ";

    /** Assertion ID field. */
    @Nonnull @NotEmpty public static final String ASSERTION_ID = "i";

    /** Assertion IssueInstant field. */
    @Nonnull @NotEmpty public static final String ASSERTION_ISSUE_INSTANT = "d";
    
    /** Request message ID field. */
    @Nonnull @NotEmpty public static final String REQUEST_ID = "I";
    
    /** Request message IssueInstant field. */
    @Nonnull @NotEmpty public static final String REQUEST_ISSUE_INSTANT = "D";

    /** InResponseTo field. */
    @Nonnull @NotEmpty public static final String IN_RESPONSE_TO = "II";

    /** Response message ID field. */
    @Nonnull @NotEmpty public static final String RESPONSE_ID = "III";

    /** Response message IssueInstant field. */
    @Nonnull @NotEmpty public static final String RESPONSE_ISSUE_INSTANT = "DD";
    
    /** Authentication timestamp field. */
    @Nonnull @NotEmpty public static final String AUTHN_INSTANT = "t";

    /** SessionIndex field. */
    @Nonnull @NotEmpty public static final String SESSION_INDEX = "x";

    /** Authentication method/context/decl field. */
    @Nonnull @NotEmpty public static final String AUTHN_CONTEXT = "ac";

    /** Status code field. */
    @Nonnull @NotEmpty public static final String STATUS_CODE = "S";

    /** Sub-status code field. */
    @Nonnull @NotEmpty public static final String SUBSTATUS_CODE = "SS";

    /** Status message field. */
    @Nonnull @NotEmpty public static final String STATUS_MESSAGE = "SM";

    /** IsPassive requested field. */
    @Nonnull @NotEmpty public static final String IS_PASSIVE = "pasv";

    /** ForceAuthn requested field. */
    @Nonnull @NotEmpty public static final String FORCE_AUTHN = "fauth";

    /** Scoping ProxyCount field. @since 4.2.0 */
    @Nonnull @NotEmpty public static final String SCOPING_PROXY_COUNT = "SCC";

    /** Scoping IdP list field. @since 4.2.0 */
    @Nonnull @NotEmpty public static final String SCOPING_IDP_LIST = "SCI";

    /** Scoping RequesterID list field. @since 4.2.0 */
    @Nonnull @NotEmpty public static final String SCOPING_REQ_LIST = "SCR";

    /** ProxyRestriction ProxyCount field. @since 4.2.0 */
    @Nonnull @NotEmpty public static final String PROXY_COUNT = "PRC";

    /** ProxyRestriction Audience field. @since 4.2.0 */
    @Nonnull @NotEmpty public static final String PROXY_AUDIENCE = "PRA";

    /** Signed inbound message field. @since 4.0.0 */
    @Nonnull @NotEmpty public static final String SIGNING = "XX";

    /** Encryption field. */
    @Nonnull @NotEmpty public static final String ENCRYPTION = "X";

    /** Encryption algorithm field. */
    @Nonnull @NotEmpty public static final String ENCRYPTION_ALGORITHM = "XA";

    /** Constructor. */
    private SAMLAuditFields() {

    }

}