package com.yitinsun.urlshortener.encoding;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62EncoderTest {

    private static final Pattern ALPHANUMERIC = Pattern.compile("^[0-9A-Za-z]+$");

    @Test
    void encodePivotIndexHasFiveToSevenAlphanumericCharacters() {
        String code = Base62Encoder.encode(56_800_235L);
        assertThat(code).matches(ALPHANUMERIC);
        assertThat(code.length()).isBetween(5, 7);
        assertThat(code).doesNotContain("+", "/");
    }

    @Test
    void encodedFormsNeverContainUrlUnsafeSymbols() {
        assertThat(Base62Encoder.encode(1L)).doesNotContain("+", "/");
        assertThat(Base62Encoder.encode(Long.MAX_VALUE)).doesNotContain("+", "/");
    }

    @Test
    void encodeMaxValueRoundTripsWithoutOverflow() {
        String encoded = Base62Encoder.encode(Long.MAX_VALUE);
        assertThat(Base62Encoder.decode(encoded)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void decodeEncodeIsIdentityForRandomPositiveIds() {
        Random random = new Random(42);
        for (int i = 0; i < 50; i++) {
            long id = Math.abs(random.nextLong());
            if (id < 0) {
                continue;
            }
            assertThat(Base62Encoder.decode(Base62Encoder.encode(id))).isEqualTo(id);
        }
        assertThat(Base62Encoder.decode(Base62Encoder.encode(0L))).isEqualTo(0L);
    }

    @Test
    void decodeRejectsInvalidCharacters() {
        assertThatThrownBy(() -> Base62Encoder.decode("+abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
