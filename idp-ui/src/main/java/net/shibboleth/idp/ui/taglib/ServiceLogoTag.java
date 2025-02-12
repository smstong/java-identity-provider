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
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.BodyContent;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.codec.HTMLEncoder;

import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

/** Logo for the SP. */
public class ServiceLogoTag extends ServiceTagSupport {

    /** Serial ID. */
    private static final long serialVersionUID = 5309357312113020929L;

    /** Class logger. */
    @Nonnull private static Logger log = LoggerFactory.getLogger(ServiceLogoTag.class);

    /** what to emit if the jsp has nothing. */
    @Nonnull @NotEmpty private static final String DEFAULT_VALUE = "";

    /** what to emit as alt txt if all else fails. */
    @Nonnull @NotEmpty private static final String DEFAULT_ALT_TXT = "SP Logo";

    /** Bean storage. Size constraint X */
    private int minWidth;

    /** Bean storage. Size constraint X */
    private int maxWidth = Integer.MAX_VALUE;

    /** Bean storage. Size constraint Y */
    private int minHeight;

    /** Bean storage. Size constraint Y */
    private int maxHeight = Integer.MAX_VALUE;

    /** Bean storage. alt text */
    @Nullable private String altTxt;

    /**
     * Set the maximum width of the logo.
     * 
     * @param value what to set
     */
    public void setMaxWidth(final Integer value) {
        maxWidth = value.intValue();
    }

    /**
     * Set the minimum width of the logo.
     * 
     * @param value what to set
     */
    public void setMinWidth(final Integer value) {
        minWidth = value.intValue();
    }

    /**
     * Set the minimum height.
     * 
     * @param value what to set
     */
    public void setMinHeight(final Integer value) {
        minHeight = value.intValue();
    }

    /**
     * Bean setter.
     * 
     * @param value what to set
     */
    public void setMaxHeight(final Integer value) {
        maxHeight = value.intValue();
    }

    /**
     * Bean setter.
     * 
     * @param value what to set
     */
    public void setAlt(final String value) {
        altTxt = value;
    }

    /**
     * Get an appropriate Logo from UIInfo.
     * 
     * @return the URL for a logo
     * 
     */
    @Nullable private String getLogoFromUIInfo() {
        final RelyingPartyUIContext ctx = getRelyingPartyUIContext();
        if (ctx == null) {
            return null;
        }
        return ctx.getLogo(minWidth, minHeight, maxWidth, maxHeight);
  
    }

    /**
     * Find what the user specified for alt txt.
     * 
     * @return the text required
     */
    @Nullable private String getAltText() {

        //
        // First see what the user tried
        //
        String value = altTxt;
        if (null != value && 0 != value.length()) {
            return value;
        }

        //
        // Try the request
        //
        value = getServiceName();
        if (null != value && 0 != value.length()) {
            return value;
        }

        return DEFAULT_ALT_TXT;
    }

    /**
     * Given the url, build an appropriate &lt;a href=...
     * 
     * @return the constructed hyperlink or null
     */
    @Nullable private String getHyperlink() {
        final String url = getLogoFromUIInfo();

        if (null == url) {
            return null;
        }

        try {
            final URI theUrl = new URI(url);
            final String scheme = theUrl.getScheme();

            if (!"http".equals(scheme) && !"https".equals(scheme) && !"data".equals(scheme)) {
                log.warn("The logo URL '{}' contained an invalid scheme (expected http:, https: or data:)", url);
                return null;
            }
        } catch (final URISyntaxException e) {
            //
            // Could not encode
            //
            log.warn("The logo URL '{}' was not a URL ", url, e);
            return null;
        }

        final String encodedURL = HTMLEncoder.encodeForHTMLAttribute(url);
        final String encodedAltTxt = HTMLEncoder.encodeForHTMLAttribute(getAltText());
        final StringBuilder sb = new StringBuilder("<img src=\"");
        sb.append(encodedURL).append('"');
        sb.append(" alt=\"").append(encodedAltTxt).append('"');
        addClassAndId(sb);
        sb.append("/>");
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override public int doEndTag() throws JspException {

        final String result = getHyperlink();

        try {
            if (null == result) {
                final BodyContent bc = getBodyContent();
                boolean written = false;
                if (null != bc) {
                    final JspWriter ew = bc.getEnclosingWriter();
                    if (ew != null) {
                        bc.writeOut(ew);
                        written = true;
                    }
                }
                if (!written) {
                    //
                    // No value provided put in our own hardwired default
                    //
                    pageContext.getOut().print(DEFAULT_VALUE);
                }
            } else {
                pageContext.getOut().print(result);
            }
        } catch (final IOException e) {
            log.warn("Error generating Logo: {}", e.getMessage());
            throw new JspException("EndTag", e);
        }
        return super.doEndTag();
    }
}
