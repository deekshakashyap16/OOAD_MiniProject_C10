package com.pes.marketplace.patterns.ocp;

class BooksPolicy extends CategoryPolicy {

    double calculateListingFee() {
        return 10.0;
    }
}
