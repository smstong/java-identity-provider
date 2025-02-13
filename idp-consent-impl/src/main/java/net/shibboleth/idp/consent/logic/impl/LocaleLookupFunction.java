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

package net.shibboleth.idp.consent.logic.impl;

import java.util.Locale;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.shibboleth.idp.profile.context.SpringRequestContext;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.RequestContext;

/**
 * Function which resolves the {@link Locale} from a {@link ProfileRequestContext}.
 */
public class LocaleLookupFunction implements Function<ProfileRequestContext, Locale> {

    /** {@inheritDoc} */
    @Nullable public Locale apply(@Nullable final ProfileRequestContext input) {
        if (input == null) {
            return null;
        }

        final SpringRequestContext springSubcontext = input.getSubcontext(SpringRequestContext.class);
        final RequestContext rc = springSubcontext != null ?  springSubcontext.getRequestContext() : null;
        if (rc != null) {
            return rc.getExternalContext().getLocale();
        }

        return null;
    }

}