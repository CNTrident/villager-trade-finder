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

    public void begin(int containerId, boolean waitForInitial, boolean waitForLocked) {
        if (pending) throw new IllegalStateException("A trade refresh is already pending");
        this.containerId = containerId;
        initialReady = !waitForInitial;
        lockedReady = !waitForLocked;
        requiresLocked = waitForLocked;
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
        // one nominal client tick of quiet even when catch-up ticks run together.
        if (!pending || !initialReady || !lockedReady
                || nowNanos - lastUpdateNanos < QUIET_PERIOD_NANOS) return false;
        pending = false;
        return true;
    }

    public boolean isPending() { return pending; }
    public boolean isWaitingForLockedOffers() { return pending && !lockedReady; }
    public void reset() { pending = false; }
}
