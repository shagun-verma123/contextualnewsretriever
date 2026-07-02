package com.example.contextualnewsretriever.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextMatchUtilTest {

    @Test
    void fullOverlapReturnsOne() {
        double score = TextMatchUtil.tokenOverlapScore("technology news", "Latest technology news today");
        assertThat(score).isEqualTo(1.0);
    }

    @Test
    void partialOverlapReturnsFraction() {
        double score = TextMatchUtil.tokenOverlapScore("sports politics", "Breaking sports update");
        assertThat(score).isEqualTo(0.5);
    }

    @Test
    void noOverlapReturnsZero() {
        double score = TextMatchUtil.tokenOverlapScore("finance", "Weather forecast for tomorrow");
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void blankInputsReturnZero() {
        assertThat(TextMatchUtil.tokenOverlapScore("", "text")).isEqualTo(0.0);
        assertThat(TextMatchUtil.tokenOverlapScore("text", "")).isEqualTo(0.0);
        assertThat(TextMatchUtil.tokenOverlapScore(null, "text")).isEqualTo(0.0);
    }
}
