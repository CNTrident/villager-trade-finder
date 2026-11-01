package com.uncraftbar.easyautocycler;

/** One outstanding request; acknowledgements complete it synchronously, in either order. */
public final class OfferUpdateState {
    private int containerId;
    private boolean pending;
    private boolean initialReady;
    private boolean lockedReady;
    private boolean requiresLocked;

    public void begin(int containerId, boolean waitForInitial, boolean waitForLocked) {
        if (pending) throw new IllegalStateException("A trade refresh is already pending");
        this.containerId = containerId;
        initialReady = !waitForInitial;
        lockedReady = !waitForLocked;
        requiresLocked = waitForLocked;
        pending = true;
    }

    public boolean onInitialOffers(int containerId) {
        if (!pending || this.containerId != containerId) return false;
        initialReady = true;
        return completeIfReady();
    }

    public boolean onLockedOffers(int containerId) {
        return onLockedOffers(containerId, true);
    }

    public boolean onLockedOffers(int containerId, boolean complete) {
        if (!pending || this.containerId != containerId) return false;
        if (!complete) {
            if (requiresLocked) lockedReady = false;
            return false;
        }
        lockedReady = true;
        return completeIfReady();
    }

    private boolean completeIfReady() {
        if (!initialReady || !lockedReady) return false;
        pending = false;
        return true;
    }

    public boolean isPending() { return pending; }
    public boolean isWaitingForLockedOffers() { return pending && !lockedReady; }
    public void reset() { pending = false; }
}
