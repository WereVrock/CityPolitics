package main.politics;

import main.pops.Pop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A political party: holds seats, view strengths, direct pop references,
 * and opinion scores toward the player and the public.
 */
public class PoliticalParty {

    private final String           name;
    private final int              seats;
    private final String           leaderName;
    private final String           personality;
    private final List<SideLeader> sideLeaders;
    private final Map<PolitcalView, ViewStrength> views;
    private final List<Pop>        memberPops;

    private int playerOpinion; // 0-100
    private int publicOpinion; // 0-100
    private int power;         // 0-100
    private int favour;        // starts 0; negative = player owes them

    public PoliticalParty(String name, int seats, int playerOpinion, int publicOpinion, int power,
                          String leaderName, String personality, List<SideLeader> sideLeaders) {
        this.name          = name;
        this.seats         = seats;
        this.playerOpinion = playerOpinion;
        this.publicOpinion = publicOpinion;
        this.power         = power;
        this.leaderName    = leaderName;
        this.personality   = personality;
        this.sideLeaders   = new ArrayList<>(sideLeaders);
        this.favour        = 0;
        this.views         = new EnumMap<>(PolitcalView.class);
        this.memberPops    = new ArrayList<>();
    }

    // ─── View Management ─────────────────────────────────────────────────────

    public void setView(PolitcalView affiliation, ViewStrength strength) {
        views.put(affiliation, strength);
    }

    public ViewStrength getViewStrength(PolitcalView affiliation) {
        return views.getOrDefault(affiliation, ViewStrength.NEUTRAL);
    }

    public Map<PolitcalView, ViewStrength> getViews() {
        return Collections.unmodifiableMap(views);
    }

    // ─── Pop Management ──────────────────────────────────────────────────────

    public void addMemberPop(Pop pop) {
        memberPops.add(pop);
    }

    public List<Pop> getMemberPops() {
        return Collections.unmodifiableList(memberPops);
    }

    public int getTotalMembers() {
        int total = 0;
        for (Pop pop : memberPops) total += pop.getCount();
        return total;
    }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public String           getName()          { return name; }
    public int              getSeats()         { return seats; }
    public String           getLeaderName()    { return leaderName; }
    public String           getPersonality()   { return personality; }
    public List<SideLeader> getSideLeaders()   { return Collections.unmodifiableList(sideLeaders); }
    public int    getPlayerOpinion() { return playerOpinion; }
    public int    getPublicOpinion() { return publicOpinion; }
    public int    getPower()         { return power; }
    public int    getFavour()        { return favour; }

    public void setPlayerOpinion(int v) { playerOpinion = Math.max(0, Math.min(100, v)); }
    public void setPublicOpinion(int v) { publicOpinion = Math.max(0, Math.min(100, v)); }
    public void setPower(int v)         { power         = Math.max(0, Math.min(100, v)); }
    public void setFavour(int v)        { favour        = v; }
}