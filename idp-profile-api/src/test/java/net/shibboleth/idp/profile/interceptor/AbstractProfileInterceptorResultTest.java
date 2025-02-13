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

package net.shibboleth.idp.profile.interceptor;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link AbstractProfileInterceptorResult} unit test. */
@SuppressWarnings("javadoc")
public class AbstractProfileInterceptorResultTest {

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testEmptyContext() {
        new MockAbstractProfileInterceptorResult("", "key", "value", Instant.ofEpochMilli(100));
    }

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testEmptyKey() {
        new MockAbstractProfileInterceptorResult("context", "", "value", Instant.ofEpochMilli(100));
    }

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testEmptyValue() {
        new MockAbstractProfileInterceptorResult("context", "key", "", Instant.ofEpochMilli(100));
    }

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testNegativeExpiration() {
        new MockAbstractProfileInterceptorResult("context", "key", "value", Instant.ofEpochMilli(-100));
    }

    @Test public void testNullExpiration() {
        final MockAbstractProfileInterceptorResult result =
                new MockAbstractProfileInterceptorResult("context", "key", "value", null);
        Assert.assertEquals(result.getStorageContext(), "context");
        Assert.assertEquals(result.getStorageKey(), "key");
        Assert.assertEquals(result.getStorageValue(), "value");
        Assert.assertEquals(result.getStorageExpiration(), null);
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testZeroExpiration() {
        new MockAbstractProfileInterceptorResult("context", "key", "value", Instant.ofEpochMilli(0));
    }

    @Test public void testResult() {
        final MockAbstractProfileInterceptorResult result =
                new MockAbstractProfileInterceptorResult("context", "key", "value", Instant.ofEpochMilli(100));
        Assert.assertEquals(result.getStorageContext(), "context");
        Assert.assertEquals(result.getStorageKey(), "key");
        Assert.assertEquals(result.getStorageValue(), "value");
        Assert.assertEquals(result.getStorageExpiration(), Instant.ofEpochMilli(100));
    }

    private class MockAbstractProfileInterceptorResult extends AbstractProfileInterceptorResult {

        public MockAbstractProfileInterceptorResult(@Nonnull @NotEmpty final String context,
                @Nonnull @NotEmpty final String key, @Nonnull @NotEmpty final String value,
                @Nullable final Instant expiration) {
            super(context, key, value, expiration);
        }
    }

}