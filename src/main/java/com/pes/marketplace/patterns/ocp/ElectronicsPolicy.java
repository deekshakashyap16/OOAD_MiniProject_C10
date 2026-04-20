package com.pes.marketplace.patterns.ocp;

class ElectronicsPolicy extends CategoryPolicy {

    double calculateListingFee() {
        return 50.0;
    }
}
