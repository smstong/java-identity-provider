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

package net.shibboleth.idp.profile.config;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.shared.annotation.ConfigurationSetting;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;

/**
 * Extension of {@link ProfileConfiguration} that adds inteceptor support.
 * 
 * @since 5.0.0
 */
public interface InterceptorAwareProfileConfiguration extends ProfileConfiguration {

    /**
     * Get an ordered list of interceptor flows to run for this profile after an inbound message is
     * decoded.
     * 
     * <p>The flow IDs returned MUST NOT contain the
     * {@link net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor#FLOW_ID_PREFIX}
     * prefix common to all interceptor flows.</p>
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return  a set of interceptor flow IDs to enable
     */
    @ConfigurationSetting(name="inboundInterceptorFlows")
    @Nonnull @NotLive @Unmodifiable List<String> getInboundInterceptorFlows(
            @Nullable final ProfileRequestContext profileRequestContext);

    /**
     * Get an ordered list of interceptor flows to run for this profile before a final outbound
     * message is generated.
     * 
     * <p>The flow IDs returned MUST NOT contain the
     * {@link net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor#FLOW_ID_PREFIX}
     * prefix common to all interceptor flows.</p>
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return  a set of interceptor flow IDs to enable
     */
    @ConfigurationSetting(name="outboundInterceptorFlows")
    @Nonnull @NotLive @Unmodifiable List<String> getOutboundInterceptorFlows(
            @Nullable final ProfileRequestContext profileRequestContext);
    
}