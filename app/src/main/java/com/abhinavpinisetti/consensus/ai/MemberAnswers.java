package com.abhinavpinisetti.consensus.ai;

import java.util.List;

/** One member's survey answers, flattened for prompt building. */
public class MemberAnswers {
    public final String name;
    public final String budget;
    public final List<String> interests;
    public final String pace;
    public final String mustDos;
    public final String avoid;

    public MemberAnswers(String name, String budget, List<String> interests, String pace,
                         String mustDos, String avoid) {
        this.name = name;
        this.budget = budget;
        this.interests = interests;
        this.pace = pace;
        this.mustDos = mustDos;
        this.avoid = avoid;
    }
}
