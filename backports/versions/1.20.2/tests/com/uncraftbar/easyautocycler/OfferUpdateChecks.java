package com.uncraftbar.easyautocycler;

/** Exercises split bursts and catch-up ticks with a deterministic clock. */
public final class OfferUpdateChecks {
    private static final long MILLIS = 1_000_000L;

    public static void main(String[] args) {
        OfferUpdateState state = new OfferUpdateState();
        state.onInitialOffers(7, 0);
        require(!state.finishIfSettled(100 * MILLIS), "unsolicited packet cannot confirm a request");

        state.begin(7, true, false);
        state.onInitialOffers(8, 100 * MILLIS);
        require(!state.finishIfSettled(200 * MILLIS), "wrong menu cannot confirm a request");
        state.onInitialOffers(7, 200 * MILLIS);
        state.onInitialOffers(7, 200 * MILLIS);
        for (int i = 0; i < 4; i++) {
            require(!state.finishIfSettled(201 * MILLIS), "catch-up ticks cannot complete the burst");
        }
        state.onInitialOffers(7, 240 * MILLIS);
        require(!state.finishIfSettled(289 * MILLIS), "third packet restarts the quiet window");
        require(state.finishIfSettled(290 * MILLIS), "the entire burst completes the request once");
        require(!state.finishIfSettled(291 * MILLIS), "the completed request cannot complete again");
        state.begin(7, true, false);
        require(!state.finishIfSettled(340 * MILLIS), "the next request needs its own response");
        state.onInitialOffers(7, 350 * MILLIS);
        require(state.finishIfSettled(400 * MILLIS), "a later round may legitimately repeat the same offers");

        state.begin(7, true, true);
        state.onLockedOffers(7, true, 400 * MILLIS);
        require(!state.finishIfSettled(500 * MILLIS), "locked-first pair still needs initial offers");
        state.onInitialOffers(7, 500 * MILLIS);
        require(state.finishIfSettled(550 * MILLIS), "locked-first pair settles");

        state.begin(7, true, true);
        state.onInitialOffers(7, 550 * MILLIS);
        state.onInitialOffers(7, 551 * MILLIS);
        require(!state.finishIfSettled(650 * MILLIS), "duplicate initial packets cannot supply locked offers");
        state.onLockedOffers(7, true, 650 * MILLIS);
        state.onLockedOffers(7, false, 651 * MILLIS);
        require(!state.finishIfSettled(750 * MILLIS), "the clear marker invalidates locked data");
        state.onLockedOffers(7, true, 750 * MILLIS);
        require(!state.finishIfSettled(799 * MILLIS), "delayed locked data also has time to settle");
        require(state.finishIfSettled(800 * MILLIS), "complete delayed locked data settles");

        state.begin(7, false, true);
        state.onLockedOffers(7, true, 800 * MILLIS);
        require(state.finishIfSettled(850 * MILLIS), "opening menu can wait for locked offers alone");

        state.begin(7, true, true);
        state.reset();
        state.onLockedOffers(7, true, 850 * MILLIS);
        state.onInitialOffers(7, 851 * MILLIS);
        require(!state.finishIfSettled(950 * MILLIS) && !state.isPending(),
                "menu replacement discards the old response");
        System.out.println("OFFER UPDATE CHECKS PASSED: duplicate bursts, catch-up ticks, menu, and locked offers");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
