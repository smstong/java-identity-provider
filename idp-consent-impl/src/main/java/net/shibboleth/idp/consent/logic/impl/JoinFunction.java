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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import com.google.common.base.Joiner;

import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Function to join the result of two functions with a separator.
 */
public class JoinFunction implements Function<ProfileRequestContext,String> {

    /** Separator. */
    @Nonnull public static final String SEPARATOR = ":";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(JoinFunction.class);

    /** First function. */
    private Function<ProfileRequestContext, String> a;

    /** Second function. */
    private Function<ProfileRequestContext, String> b;

    /** Joiner to join results. */
    @Nonnull private final Joiner joiner;

    /**
     * 
     * Constructor.
     *
     * @param functionA the first function
     * @param functionB the second function
     */
    public JoinFunction(@Nonnull final Function<ProfileRequestContext, String> functionA,
            @Nonnull final Function<ProfileRequestContext, String> functionB) {
        Constraint.isNotNull(functionA, "Function A cannot be null");
        Constraint.isNotNull(functionB, "Function B cannot be null");

        a = functionA;
        b = functionB;

        joiner = Joiner.on(SEPARATOR).skipNulls();
    }

    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final ProfileRequestContext input) {
        if (input == null) {
            return null;
        }

        final String resultA = a.apply(input);
        final String resultB = b.apply(input);

        final String result = joiner.join(resultA, resultB);
        log.debug("Result '{}'", result);
        return result;
    }

}