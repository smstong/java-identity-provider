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

import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.or;

import java.util.Set;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.logic.ExceptionPredicate;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/** Unit test for {@link AttributeValuePredicateMatcher}. */
public class AttributeValuePredicateMatcherTest extends AbstractMatcherTest {

    @BeforeTest public void setup() throws Exception {
        super.setUp();
    }

    @Test public void testNullArguments() throws Exception {
        AttributeValuePredicateMatcher matcher = new AttributeValuePredicateMatcher(alwaysTrue());

        boolean thrown = false;
        try {
            matcher.getMatchingValues(null, filterContext);
        } catch (ConstraintViolationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        thrown = false;
        try {
            matcher.getMatchingValues(attribute, null);
        } catch (ConstraintViolationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        thrown = false;
        try {
            matcher.getMatchingValues(null, null);
        } catch (ConstraintViolationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        thrown = false;
        try {
            new AttributeValuePredicateMatcher(null);
        } catch (ConstraintViolationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);
    }

    @Test public void testGetMatchingValues() throws AttributeFilteringException {
        AttributeValuePredicateMatcher matcher =
                new AttributeValuePredicateMatcher(or(equalTo(value1), equalTo(value2)));

        Set<AttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(value1) && result.contains(value2));

    }

    @Test public void testFailingGetMatchingValues() {
        AttributeValuePredicateMatcher matcher =
                new AttributeValuePredicateMatcher(new ExceptionPredicate(new RuntimeException()));

        try {
            matcher.getMatchingValues(attribute, filterContext);
            Assert.fail();
        } catch (AttributeFilteringException e) {
            // OK
        }

    }

    @Test public void testEqualsHashToString() {
        AttributeValuePredicateMatcher matcher =
                new AttributeValuePredicateMatcher(or(equalTo(value1), equalTo(value2)));

        matcher.toString();

        Assert.assertFalse(matcher.equals(null));
        Assert.assertTrue(matcher.equals(matcher));
        Assert.assertFalse(matcher.equals(this));

        AttributeValuePredicateMatcher other = new AttributeValuePredicateMatcher(or(equalTo(value1), equalTo(value2)));

        Assert.assertTrue(matcher.equals(other));
        Assert.assertEquals(matcher.hashCode(), other.hashCode());

        other = new AttributeValuePredicateMatcher(or(equalTo(value2), equalTo(value1)));

        Assert.assertFalse(matcher.equals(other));
        Assert.assertNotSame(matcher.hashCode(), other.hashCode());

    }
}