package skinsmarket.demo.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfoValidatorTest {

    @Test
    void passwordIsInvalidWhenRepeatIsMissing() {
        assertFalse(InfoValidator.isValidPassword("pass123", null));
    }

    @Test
    void passwordIsValidWhenItMatchesRulesAndRepeat() {
        assertTrue(InfoValidator.isValidPassword("pass123", "pass123"));
    }

    @Test
    void emailIsValidWhenItHasCommonRealFormat() {
        assertTrue(InfoValidator.isValidEmail("User.Name+tag@gmail.com"));
    }

    @Test
    void emailIsInvalidWhenDomainHasNoPublicSuffix() {
        assertFalse(InfoValidator.isValidEmail("usuario@mail"));
    }

    @Test
    void emailIsInvalidWhenDomainLabelStartsWithHyphen() {
        assertFalse(InfoValidator.isValidEmail("usuario@-mail.com"));
    }

    @Test
    void emailIsNormalizedBeforePersistingOrLogin() {
        assertTrue(InfoValidator.isValidEmail("  USER@MAIL.COM  "));
        assertTrue(InfoValidator.normalizeEmail("  USER@MAIL.COM  ").equals("user@mail.com"));
    }
}
