package io.github.liulzz.resourcepressure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PercentParserTest {
    @Test
    void parsesAsciiPercent() {
        assertThat(PercentParser.parsePercent("20%")).isEqualTo(20d);
    }

    @Test
    void parsesFullWidthPercentAndPlusMinus() {
        assertThat(PercentParser.parsePercent(" ±10％ ")).isEqualTo(10d);
    }

    @Test
    void parsesNullBlankPlainAndPlusMinusValues() {
        assertThat(PercentParser.parsePercent(null)).isZero();
        assertThat(PercentParser.parsePercent("  ")).isZero();
        assertThat(PercentParser.parsePercent("50")).isEqualTo(50d);
        assertThat(PercentParser.parsePercent("+/-25%")).isEqualTo(25d);
    }

    @Test
    void rejectsInvalidPercent() {
        assertThatThrownBy(() -> PercentParser.parsePercent("abc"))
                .isInstanceOf(NumberFormatException.class);
    }
}
