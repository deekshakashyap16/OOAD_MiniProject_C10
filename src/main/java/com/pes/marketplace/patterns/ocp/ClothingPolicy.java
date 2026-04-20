package com.pes.marketplace.patterns.ocp;

class ClothingPolicy extends CategoryPolicy {

    double calculateListingFee() {
        return 20.0;
    }
}
