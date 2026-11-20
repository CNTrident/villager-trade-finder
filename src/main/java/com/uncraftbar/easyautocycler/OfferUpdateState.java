package com.uncraftbar.easyautocycler;

/** Tracks one request while its menu receives a burst of offer updates. */
public final class OfferUpdateState {
    private static final long QUIET_PERIOD_NANOS = 50_000_000L;

    private int containerId;
    private boolean pending;
    private boolean initialReady;
    private boolean lockedReady;
    private boolean requiresLocked;
    private long lastUpdateNanos;
    private long expectedRound;
    private boolean confirmed;

    public void begin(int containerId, boolean waitForInitial, boolean waitForLocked) {
        begin(containerId, waitForInitial, waitForLocked, 0);
    }

    public void begin(int containerId, boolean waitForInitial, boolean waitForLocked, long expectedRound) {
        if (pending) throw new IllegalStateException("A trade refresh is already pending");
        this.containerId = containerId;
        initialReady = !waitForInitial;
        lockedReady = !waitForLocked;
        requiresLocked = waitForLocked;
        this.expectedRound = expectedRound;
        confirmed = false;
        pending = true;
    }

    public void onInitialOffers(int containerId, long nowNanos) {
        if (!pending || this.containerId != containerId) return;
        initialReady = true;
        lastUpdateNanos = nowNanos;
    }

    public void onLockedOffers(int containerId, boolean complete, long nowNanos) {
        if (!pending || this.containerId != containerId || !requiresLocked) return;
        lockedReady = complete;
        lastUpdateNanos = nowNanos;
    }

    public boolean finishIfSettled(long nowNanos) {
        // A menu ID cannot separate replies to consecutive requests. Require
        // a quiet window only when the server cannot confirm a request ID.
        return finish(expectedRound == 0 && nowNanos - lastUpdateNanos >= QUIET_PERIOD_NANOS);
    }

    public boolean expectsConfirmation(int containerId, long round) {
        return pending && !confirmed && round != 0 && round == expectedRound && containerId == this.containerId;
    }

    public void onConfirmation(int containerId, long round) {
        if (expectsConfirmation(containerId, round)) confirmed = true;
    }

    public boolean finishIfConfirmed() {
        return finish(confirmed);
    }

    private boolean finish(boolean ready) {
        if (!pending || !ready || !initialReady || !lockedReady) return false;
        pending = false;
        return true;
    }

    public boolean isPending() { return pending; }
    public boolean isWaitingForLockedOffers() { return pending && !lockedReady; }
    public void reset() { pending = false; }
}
