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

package net.shibboleth.idp.saml.authn.principal;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.principal.CloneablePrincipal;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.xml.SerializeSupport;

import org.opensaml.core.xml.XMLRuntimeException;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.core.xml.util.XMLObjectSupport.CloneOutputOption;
import org.opensaml.saml.saml2.core.AuthnContextDecl;

import com.google.common.base.MoreObjects;

/** Principal based on a SAML AuthnContextDecl. */
public final class AuthnContextDeclPrincipal implements CloneablePrincipal {

    /** The declaration. */
    @Nonnull private AuthnContextDecl authnContextDecl;

    /** Serialized form of declaration. */
    @Nonnull @NotEmpty private String name;
    
    /**
     * Constructor.
     * 
     * @param decl the declaration
     * 
     * @throws MarshallingException if an error occurs marshalling the declaration into string form
     */
    public AuthnContextDeclPrincipal(@Nonnull @ParameterName(name="decl") final AuthnContextDecl decl)
            throws MarshallingException {
        authnContextDecl = Constraint.isNotNull(decl, "AuthnContextDeclRef cannot be null");
        name = SerializeSupport.nodeToString(Constraint.isNotNull(XMLObjectSupport.getMarshaller(decl),
                "No marshaller for AuthnContextDecl").marshall(decl));
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getName() {
        return name;
    }
    
    /**
     * Returns the value as a SAML {@link AuthnContextDecl}.
     * 
     * @return  the principal value in the form of an {@link AuthnContextDecl}
     */
    @Nonnull public AuthnContextDecl getAuthnContextDecl() {
        return authnContextDecl;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return authnContextDecl.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        }

        if (this == other) {
            return true;
        }

        if (other instanceof AuthnContextDeclPrincipal) {
            return name.equals(((AuthnContextDeclPrincipal) other).getName());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("authnContextDecl", name).toString();
    }

    /** {@inheritDoc} */
    @Nonnull public AuthnContextDeclPrincipal clone() throws CloneNotSupportedException {
        final AuthnContextDeclPrincipal copy = (AuthnContextDeclPrincipal) super.clone();
        try {
            copy.authnContextDecl = XMLObjectSupport.cloneXMLObject(authnContextDecl, 
                    CloneOutputOption.RootDOMInNewDocument);
            copy.name = SerializeSupport.nodeToString(
                    Constraint.isNotNull(XMLObjectSupport.getMarshaller(copy.authnContextDecl),
                            "No marshaller for AuthnContextDecl").marshall(copy.authnContextDecl));
        } catch (final MarshallingException | UnmarshallingException e) {
            throw new XMLRuntimeException(e);
        }
        return copy;
    }
}