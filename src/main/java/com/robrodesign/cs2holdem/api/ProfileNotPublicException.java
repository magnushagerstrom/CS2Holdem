package com.robrodesign.cs2holdem.api;

/**
 * Only players who registered themselves have a public profile. Everyone else we may know about
 * (because they played with a registered friend) is just a name in a match.
 *
 * Gives 404, not 403: a 403 would tell a visitor "we have data on this person", which is
 * exactly what we do not want to say about someone who never signed up.
 */
public class ProfileNotPublicException extends RuntimeException {
    public ProfileNotPublicException() {
        super("This player has no public profile");
    }
}
