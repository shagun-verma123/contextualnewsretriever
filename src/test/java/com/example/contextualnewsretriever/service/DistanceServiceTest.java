package com.example.contextualnewsretriever.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceServiceTest {

    private final DistanceService distanceService = new DistanceService();

    @Test
    void sameCoordinatesReturnZeroDistance() {
        double distance = distanceService.haversineKm(28.6139, 77.2090, 28.6139, 77.2090);
        assertThat(distance).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void knownDistanceBetweenDelhiAndMumbaiIsApproximatelyCorrect() {
        // Delhi to Mumbai is ~1150km great-circle distance.
        double distance = distanceService.haversineKm(28.6139, 77.2090, 19.0760, 72.8777);
        assertThat(distance).isBetween(1100.0, 1200.0);
    }
}
