package com.pes.marketplace.patterns.ocp;

class DefaultPolicy extends CategoryPolicy {

    double calculateListingFee() {
        return 15.0;
    }
}
