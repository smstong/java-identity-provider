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

package net.shibboleth.idp.authn.spnego.impl;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.shibboleth.shared.net.CookieManager;
import net.shibboleth.shared.testing.ConstantSupplier;

@SuppressWarnings("javadoc")
public class SPNEGOAutoLoginManagerTest {

    /**
     * SPNEGO auto-login cookie value representing false.
     */
    protected static final String AUTOLOGIN_COOKIE_VALUE_FALSE = "false";

    /**
     * SPNEGO auto-login cookie value representing "ignored value".
     */
    protected static final String AUTOLOGIN_COOKIE_VALUE_OTHER = "other";

    @BeforeMethod
    public void setUp() throws Exception {
    }

    protected SPNEGOAutoLoginManager createAutoLoginManager(MockHttpServletRequest req, MockHttpServletResponse res)
            throws Exception {
        CookieManager cookieManager = new CookieManager();
        final HttpServletRequest request = req != null ? req : new MockHttpServletRequest();
        final HttpServletResponse response = res != null ? res : new MockHttpServletResponse();
        
        cookieManager.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
        cookieManager.setHttpServletResponseSupplier(new ConstantSupplier<>(response));
        cookieManager.initialize();

        SPNEGOAutoLoginManager autoLoginManager = new SPNEGOAutoLoginManager();
        autoLoginManager.setCookieManager(cookieManager);
        autoLoginManager.setCookieName("spnego_autologin");
        autoLoginManager.initialize();

        return autoLoginManager;
    }

    @Test
    public void enableAutoLogin_shouldSetCookieToTrue() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        SPNEGOAutoLoginManager autoLoginManager = createAutoLoginManager(null, res);

        autoLoginManager.enable();

        Cookie cookie = res.getCookie("spnego_autologin");
        assert cookie != null;
        Assert.assertEquals(cookie.getValue(), SPNEGOAutoLoginManager.AUTOLOGIN_COOKIE_VALUE_TRUE);
    }

    @Test
    public void disableAutoLogin_shouldUnsetCookie() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        SPNEGOAutoLoginManager autoLoginManager = createAutoLoginManager(null, res);

        autoLoginManager.disable();

        Cookie cookie = res.getCookie("spnego_autologin");
        assert cookie != null;
        Assert.assertNull(cookie.getValue());
    }

    @Test
    public void givenCookieTrue_onlyIsEnabledShouldReturnTrue() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("spnego_autologin",
                SPNEGOAutoLoginManager.AUTOLOGIN_COOKIE_VALUE_TRUE));
        SPNEGOAutoLoginManager autoLoginManager = createAutoLoginManager(req, null);

        Assert.assertTrue(autoLoginManager.isEnabled());
        Assert.assertFalse(autoLoginManager.isDisabled());
    }

    @Test
    public void givenCookieAbsent_onlyIsDisabledShouldReturnTrue() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        SPNEGOAutoLoginManager autoLoginManager = createAutoLoginManager(req, null);

        Assert.assertFalse(autoLoginManager.isEnabled());
        Assert.assertTrue(autoLoginManager.isDisabled());
    }

    @Test
    public void givenCookieFalse_onlyIsDisabledShouldReturnTrue() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("spnego_autologin", AUTOLOGIN_COOKIE_VALUE_FALSE));
        SPNEGOAutoLoginManager autoLoginManager = createAutoLoginManager(req, null);

        Assert.assertFalse(autoLoginManager.isEnabled());
        Assert.assertTrue(autoLoginManager.isDisabled());
    }

    @Test
    public void givenCookieOtherValue_onlyIsDisabledShouldReturnTrue() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("spnego_autologin", AUTOLOGIN_COOKIE_VALUE_OTHER));
        SPNEGOAutoLoginManager autoLoginManager = createAutoLoginManager(req, null);

        Assert.assertFalse(autoLoginManager.isEnabled());
        Assert.assertTrue(autoLoginManager.isDisabled());
    }

}
