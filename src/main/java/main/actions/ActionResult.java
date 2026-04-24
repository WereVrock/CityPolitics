package main.actions;

import main.politics.VoteResult;

/**
 * Immutable result of attempting a player action.
 */
public class ActionResult {

    public enum Kind { OK, FAIL, VOTE_PASSED, VOTE_FAILED, VOTE_PENDING }

    private final Kind       kind;
    private final String     message;
    private final VoteResult voteResult; // null for non-formal actions

    private ActionResult(Kind kind, String message, VoteResult voteResult) {
        this.kind       = kind;
        this.message    = message;
        this.voteResult = voteResult;
    }

    public static ActionResult ok(String message) {
        return new ActionResult(Kind.OK, message, null);
    }

    public static ActionResult fail(String message) {
        return new ActionResult(Kind.FAIL, message, null);
    }

    public static ActionResult votePassed(String message, VoteResult vote) {
        return new ActionResult(Kind.VOTE_PASSED, message, vote);
    }

    public static ActionResult voteFailure(String message, VoteResult vote) {
        return new ActionResult(Kind.VOTE_FAILED, message, vote);
    }

    public static ActionResult votePending(String message) {
        return new ActionResult(Kind.VOTE_PENDING, message, null);
    }

    public boolean    isSuccess()   { return kind == Kind.OK || kind == Kind.VOTE_PASSED; }
    public boolean    isPending()   { return kind == Kind.VOTE_PENDING; }
    public boolean    hasVote()     { return voteResult != null; }
    public Kind       getKind()     { return kind; }
    public String     getMessage()  { return message; }
    public VoteResult getVoteResult(){ return voteResult; }

    @Override
    public String toString() {
        return "[" + kind + "] " + message;
    }
}