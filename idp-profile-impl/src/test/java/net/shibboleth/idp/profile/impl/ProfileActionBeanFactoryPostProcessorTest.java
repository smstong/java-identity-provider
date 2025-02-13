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

package net.shibboleth.idp.profile.impl;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.shared.annotation.Prototype;

/** {@link ProfileActionBeanFactoryPostProcessor} unit test. */
@SuppressWarnings("javadoc")
@ContextConfiguration({"ProfileActionBeanFactoryPostProcessorTest.xml"})
public class ProfileActionBeanFactoryPostProcessorTest extends AbstractTestNGSpringContextTests {

    @Test public void testPostProcessBeanFactory() {
        final ApplicationContext ac = applicationContext;
        assert  ac != null;
        Assert.assertTrue(ac.isPrototype("MockIdPActionWithoutScopeProperty"));
        Assert.assertTrue(ac.isPrototype("MockPrototypeAnnotatedIdPActionWithoutScopeProperty"));
        Assert.assertTrue(ac.isPrototype("MockOpenSAMLActionWithoutScopeProperty"));
        Assert.assertTrue(ac.isPrototype("MockPrototypeAnnotatedOpenSAMLActionWithoutScopeProperty"));
    }

    public static class MockIdPAction extends net.shibboleth.idp.profile.AbstractProfileAction {
    }

    public static class MockOpenSAMLAction extends org.opensaml.profile.action.AbstractProfileAction {
    }

    @Prototype
    public static class MockPrototypeAnnotatedIdPAction extends net.shibboleth.idp.profile.AbstractProfileAction {
    }

    @Prototype
    public static class MockPrototypeAnnotatedOpenSAMLAction extends org.opensaml.profile.action.AbstractProfileAction {
    }
}
