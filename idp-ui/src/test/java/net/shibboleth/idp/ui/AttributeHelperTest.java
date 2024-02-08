package net.shibboleth.idp.ui;

import static org.testng.Assert.assertEquals;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.resolver.testing.ResolverTestSupport;
import net.shibboleth.idp.ui.impl.AttributeHelper;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;

public class AttributeHelperTest {

    @Test public void TestHelper() throws ComponentInitializationException {
        final ProfileRequestContext prc = new ProfileRequestContext();
        final AttributeContext ac = prc.ensureSubcontext(RelyingPartyContext.class).ensureSubcontext(AttributeContext.class);
        final IdPAttribute a1 = ResolverTestSupport.buildAttribute("A1", "A1Value1", "Value2");
        final IdPAttribute a2 = ResolverTestSupport.buildAttribute("A2", "A2Value1");
        final AttributeHelper ah = new AttributeHelper();
        ah.setId("AttributeHelper");
        ah.initialize();

        ac.setUnfilteredIdPAttributes(CollectionSupport.arrayAsList(a1, a2));
        ac.setIdPAttributes(CollectionSupport.singleton(a1));

        assertEquals(ah.getFirstAttributeValue(prc, "A1"), "A1Value1");
        assertEquals(ah.getFirstAttributeValue(prc, "A2"), "");
        assertEquals(ah.getFirstAttributeValue(prc, "A3", "Nothing"), "Nothing");

        assertEquals(ah.getFirstUnfilteredAttributeValue(prc, "A1"), "A1Value1");
        assertEquals(ah.getFirstUnfilteredAttributeValue(prc, "A2"), "A2Value1");
        assertEquals(ah.getFirstAttributeValue(prc, "A3"), "");

    }
}
