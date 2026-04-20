package com.pes.marketplace.patterns.ocp;

class StationeryPolicy extends CategoryPolicy {

    double calculateListingFee() {
        return 5.0;
    }
}
