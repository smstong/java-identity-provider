/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.filtering.impl.matcher;

import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;

import com.google.common.base.Predicate;

/**
 * A compound Interface which implements both PolicyRequirementRules and AttributeRules. The former is implemented via
 * the {@link Predicate<AttributeFilterContext>} interface The later via the {@link AttributeValueMatcher} 
 * interface.<br/>
 * 
 * Thus anything which implements this can be plugged in to an
 * {@link net.shibboleth.idp.attribute.filtering.AttributeFilterPolicy#activationCriteria} or into a
 * {@link net.shibboleth.idp.attribute.filtering.AttributeValueFilterPolicy#valueMatchingRule}<br/>
 * 
 * This therefore reflects the schema. 
 */
public interface MatchFunctor extends Predicate<AttributeFilterContext>, AttributeValueMatcher {

}
