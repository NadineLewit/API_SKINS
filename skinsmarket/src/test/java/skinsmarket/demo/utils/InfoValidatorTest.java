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
}
