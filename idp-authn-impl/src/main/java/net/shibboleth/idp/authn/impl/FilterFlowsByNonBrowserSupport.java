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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * An authentication action that filters out potential authentication flows if the request requires
 * non-browser support and the flows require a browser.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class) != null</pre>
 * @post AuthenticationContext.getPotentialFlows() is modified as above.
 */
public class FilterFlowsByNonBrowserSupport extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(FilterFlowsByNonBrowserSupport.class);

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }

        if (profileRequestContext.isBrowserProfile()) {
            log.debug("{} Request does not have non-browser requirement, nothing to do", getLogPrefix());
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final Map<String, AuthenticationFlowDescriptor> potentialFlows = authenticationContext.getPotentialFlows();

        final Iterator<Entry<String, AuthenticationFlowDescriptor>> descriptorItr =
                potentialFlows.entrySet().iterator();
        while (descriptorItr.hasNext()) {
            final AuthenticationFlowDescriptor descriptor = descriptorItr.next().getValue();
            if (descriptor.isNonBrowserSupported()) {
                log.debug("{} Retaining flow {}, it supports non-browser authentication", getLogPrefix(),
                        descriptor.getId());
            } else {
                log.debug("{} Removing flow {}, it does not support non-browser authentication", getLogPrefix(),
                        descriptor.getId());
                descriptorItr.remove();
            }
        }

        if (potentialFlows.size() == 0) {
            log.info("{} No potential authentication flows remain after filtering", getLogPrefix());
        } else {
            log.debug("{} Potential authentication flows left after filtering: {}", getLogPrefix(),
                    potentialFlows.keySet());
        }
    }
    
}