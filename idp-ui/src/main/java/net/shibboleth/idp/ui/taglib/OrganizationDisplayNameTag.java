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

package net.shibboleth.idp.ui.taglib;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.shared.codec.HTMLEncoder;
import net.shibboleth.shared.primitive.LoggerFactory;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.BodyContent;

/** Service OrganizationDisplayName - directly from the metadata if present. */
public class OrganizationDisplayNameTag extends ServiceTagSupport {

    /** Serial ID. */
    private static final long serialVersionUID = -196716418770324981L;

    /** Class logger. */
    @Nonnull private static Logger log = LoggerFactory.getLogger(OrganizationDisplayNameTag.class);

    /**
     * look for the &lt;OrganizationDisplayName&gt;.
     * 
     * @return null or an appropriate string
     */
    @Nullable private String getOrganizationDisplayName() {
        final RelyingPartyUIContext ctx = getRelyingPartyUIContext();
        if (ctx == null) {
            return null;
        }
        return ctx.getOrganizationDisplayName();
    }

    /** {@inheritDoc} */
    @Override public int doEndTag() throws JspException {

        final String name = getOrganizationDisplayName();

        try {
            if (null == name) {
                final BodyContent bc = getBodyContent();
                if (null != bc) {
                    final JspWriter ew = bc.getEnclosingWriter();
                    if (ew != null) {
                        bc.writeOut(ew);
                    }
                }
            } else {
                pageContext.getOut().print(HTMLEncoder.encodeForHTML(name));
            }
        } catch (final IOException e) {
            log.warn("Error generating OrganizationDisplayName: {}", e.getMessage());
            throw new JspException("EndTag", e);
        }
        return super.doEndTag();
    }

}