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

package net.shibboleth.idp.authn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * A base class for authentication actions that extract usernames for subsequent use.
 * 
 * <p>The base class adds a common mechanism for applying regular expression transforms to
 * the username prior to being added to the context tree.</p>
 */
public abstract class AbstractExtractionAction extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractExtractionAction.class);
    
    /** Match patterns and replacement strings to apply. */
    @Nonnull private List<Pair<Pattern,String>> transforms;

    /** Convert to uppercase prior to transforms? */
    private boolean uppercase;
    
    /** Convert to lowercase prior to transforms? */
    private boolean lowercase;
    
    /** Trim prior to transforms? */
    private boolean trim;
    
    /** Generic hook for remapping username. */
    @Nullable private BiFunction<ProfileRequestContext,String,String> usernameRemappingStrategy; 
    
    /** Constructor. */
    public AbstractExtractionAction() {
        transforms = CollectionSupport.emptyList();
        
        uppercase = false;
        lowercase = false;
        trim = false;
    }

    /**
     * A collection of regular expression and replacement pairs.
     * 
     * @param newTransforms collection of replacement transforms
     */
    public void setTransforms(@Nullable final Collection<Pair<String, String>> newTransforms) {
        checkSetterPreconditions();
        if (newTransforms != null) {
            transforms = new ArrayList<>();
            for (final Pair<String,String> p : newTransforms) {
                final Pattern pattern = Pattern.compile(StringSupport.trimOrNull(p.getFirst()));
                transforms.add(new Pair<>(pattern, Constraint.isNotNull(
                        StringSupport.trimOrNull(p.getSecond()), "Replacement expression cannot be null")));
            }
        } else {
            transforms = CollectionSupport.emptyList();
        }
    }

    /**
     * Controls conversion to uppercase prior to applying any transforms.
     * 
     * @param flag  uppercase flag
     */
    public void setUppercase(final boolean flag) {
        checkSetterPreconditions();
        uppercase = flag;
    }

    /**
     * Controls conversion to lowercase prior to applying any transforms.
     * 
     * @param flag lowercase flag
     */
    public void setLowercase(final boolean flag) {
        checkSetterPreconditions();
        lowercase = flag;
    }
    
    /**
     * Controls whitespace trimming prior to applying any transforms.
     * 
     * @param flag trim flag
     */
    public void setTrim(final boolean flag) {
        checkSetterPreconditions();
        trim = flag;
    }
    
    /**
     * Sets a general hook for remapping username.
     * 
     * @param strategy username remapping strategy
     * 
     * @since 5.1.0
     */
    public void setUsernameRemappingStrategy(@Nullable final BiFunction<ProfileRequestContext,String,String> strategy) {
        checkSetterPreconditions();
        
        usernameRemappingStrategy = strategy;
    }
    
    /**
     * Apply any configured rules, regular expression replacements, or remapping strategy
     * to an input value and return the result.
     * 
     * @param input the input string
     * 
     * @return  the result of applying the rules
     * 
     * @deprecated
     */
    @Deprecated(since="5.1.0", forRemoval=true)
    @Nullable @NotEmpty protected String applyTransforms(@Nullable final String input) {
        
        // No deprecation warning due to legacy plugins unable to convert their calls.
        return applyTransforms(null, input);
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /**
     * Apply any configured rules, regular expression replacements, or remapping strategy
     * to an input value and return the result.
     * 
     * @param profileRequestContext profile request context
     * @param input the input string
     * 
     * @return  the result of applying the rules
     * 
     * @since 5.1.0
     */
    @Nullable @NotEmpty protected String applyTransforms(@Nullable final ProfileRequestContext profileRequestContext,
            @Nullable final String input) {
        if (input == null) {
            return usernameRemappingStrategy != null ?
                    usernameRemappingStrategy.apply(profileRequestContext, input) : null;
        }
        
        String s = input;
        
        if (trim) {
            log.debug("{} Trimming whitespace of input string '{}'", getLogPrefix(), s);
            s = s.trim();
        }
        
        if (lowercase) {
            log.debug("{} Converting input string '{}' to lowercase", getLogPrefix(), s);
            s = s.toLowerCase();
        } else if (uppercase) {
            log.debug("{} Converting input string '{}' to uppercase", getLogPrefix(), s);
            s = s.toUpperCase();
        }
        
        if (transforms.isEmpty()) {
            return usernameRemappingStrategy != null ? usernameRemappingStrategy.apply(profileRequestContext, s) : s;
        }
        
        for (final Pair<Pattern,String> p : transforms) {
            final Pattern pattern = p.getFirst();
            if (pattern != null) {
                final Matcher m = pattern.matcher(s);
                log.debug("{} Applying replacement expression '{}' against input '{}'", getLogPrefix(),
                        pattern.pattern(), s);
                s = m.replaceAll(p.getSecond());
                log.debug("{} Result of replacement is '{}'", getLogPrefix(), s);
            }
        }

        return usernameRemappingStrategy != null ? usernameRemappingStrategy.apply(profileRequestContext, s) : s;
    }
// Checkstyle: CyclomaticComplexity ON

}