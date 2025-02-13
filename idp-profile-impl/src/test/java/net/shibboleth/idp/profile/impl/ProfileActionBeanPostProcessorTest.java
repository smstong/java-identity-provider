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

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.shared.component.ComponentInitializationException;

/** {@link ProfileActionBeanPostProcessor} unit test. */
@ContextConfiguration({"ProfileActionBeanPostProcessorTest.xml"})
@SuppressWarnings("javadoc")
public class ProfileActionBeanPostProcessorTest extends AbstractTestNGSpringContextTests {

    @Test public void testPostProcessAfterInitialization() {
        final ApplicationContext ac = applicationContext;
        assert  ac != null;

        Object bean = null;
        
        bean = ac.getBean("IdPActionWithDefaultID");
        Assert.assertFalse(bean instanceof WebFlowProfileActionAdaptor);

        bean = ac.getBean("OpenSAMLActionWithDefaultID");
        Assert.assertTrue(bean instanceof WebFlowProfileActionAdaptor);
        Assert.assertTrue(((WebFlowProfileActionAdaptor) bean).isInitialized());
    }

    @Test(expectedExceptions = BeanCreationException.class) public void testBeanCreationException() {
        assert applicationContext!= null;
        applicationContext.getBean("OpenSAMLExceptionAction");
    }

    public static class MockIdPAction extends net.shibboleth.idp.profile.AbstractProfileAction {

    }

    public static class MockOpenSAMLAction extends org.opensaml.profile.action.AbstractProfileAction {

    }

    public static class MockOpenSAMLExceptionAction extends org.opensaml.profile.action.AbstractProfileAction {
        @Override
        protected void doInitialize() throws ComponentInitializationException {
            throw new ComponentInitializationException();
        }
    }

}
