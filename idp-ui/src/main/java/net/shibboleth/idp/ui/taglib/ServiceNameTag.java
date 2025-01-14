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

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.BodyContent;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.codec.HTMLEncoder;

import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;


/**
 * Display the serviceName.
 * 
 * This is taken in order
 *  1) From the mdui
 *  2) AttributeConsumeService
 *  3) HostName from the EntityId
 *  4) EntityId.
 */
public class ServiceNameTag extends ServiceTagSupport {

    /** Serial ID. */
    private static final long serialVersionUID = 2131709003267781456L;

    /** Class logger. */
    @Nonnull private static Logger log = LoggerFactory.getLogger(ServiceNameTag.class);
    
    /** what to emit if the jsp has nothing. */
    @Nonnull @NotEmpty private static final String DEFAULT_VALUE = "Unspecified Service Provider";

    /** Bean storage for default value. */
    @Nullable private String defaultValue;

    /**
     * Set the default value.
     * 
     * @param value what to set
     */
    public void setDefaultValue(@Nullable final String value) {
        defaultValue = value;
    }
    
    @Override
    public int doStartTag() throws JspException {
       
        try {
            final String rawServiceName = getServiceName();
            
            final String serviceName = HTMLEncoder.encodeForHTML(rawServiceName);
            
            if (null == serviceName) {
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
                    pageContext.getOut().print(defaultValue != null ? defaultValue : DEFAULT_VALUE);
                }
            } else {
                pageContext.getOut().print(serviceName);
            }
        } catch (final IOException e) {
            log.warn("Error generating name: {}", e.getMessage());
            throw new JspException("StartTag", e);
        }
        return super.doStartTag();
    }

}