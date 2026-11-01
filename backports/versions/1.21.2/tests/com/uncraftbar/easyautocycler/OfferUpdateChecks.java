package com.uncraftbar.easyautocycler;

/** Regression checks for packet ordering and immediate completion, without a game tick. */
public final class OfferUpdateChecks {
    public static void main(String[] args) {
        OfferUpdateState state = new OfferUpdateState();
        require(!state.onInitialOffers(7), "unsolicited packet must not trigger a refresh");
        state.begin(7, true, false);
        require(!state.onInitialOffers(8) && state.isPending(), "wrong container must be ignored");
        require(state.onInitialOffers(7) && !state.isPending(), "initial offers complete inline");
        require(!state.onInitialOffers(7), "duplicate acknowledgement must not complete twice");

        state.begin(7, true, true);
        require(!state.onLockedOffers(7), "locked-first response must wait for its initial offers");
        require(state.onInitialOffers(7), "locked-first pair completes inline on second packet");
        state.begin(7, true, true);
        require(!state.onInitialOffers(7), "initial-first response must not use stale locked offers");
        require(!state.onInitialOffers(7), "duplicate initial packet is not a locked response");
        require(state.onLockedOffers(7), "initial-first pair completes inline on second packet");

        state.begin(7, true, true);
        require(!state.onLockedOffers(7, false), "VisibleTraders clear marker is not completion");
        require(!state.onInitialOffers(7), "initial packet must not complete a clear-only response");
        require(state.isPending(), "generation must stay pending until actual locked data arrives");
        require(state.onLockedOffers(7, true), "actual delayed locked data completes the round");

        state.begin(7, false, true);
        require(state.onLockedOffers(7), "opening-menu locked offers require no extra tick");
        state.begin(7, true, true);
        state.reset();
        require(!state.onLockedOffers(7) && !state.onInitialOffers(7), "stop discards pending response");
        for (int i = 0; i < 100_000; i++) {
            state.begin(7, true, true);
            require(!state.onInitialOffers(7) && state.onLockedOffers(7), "unthrottled consecutive rounds");
        }
        System.out.println("OFFER UPDATE CHECKS PASSED: packet ordering, stop, and 100000 rounds without ticks");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
