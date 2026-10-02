package com.abhinavpinisetti.consensus.ai;

import java.util.List;

/** Builds the text prompts sent to Gemini. Pure Java so it can be unit tested. */
public final class PromptBuilder {

    private PromptBuilder() {}

    public static String itineraryPrompt(String destination, String startDate, String endDate,
                                         int dayCount, int groupSize, List<MemberAnswers> answers) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are planning a group trip that everyone in the group will enjoy.\n\n")
                .append("Destination: ").append(destination).append('\n')
                .append("Dates: ").append(startDate).append(" to ").append(endDate)
                .append(" (").append(dayCount).append(" days)\n")
                .append("Group size: ").append(groupSize).append(" people, ")
                .append(answers.size()).append(" of whom answered the survey.\n\n")
                .append("Each member's preferences:\n");
        appendAnswers(sb, answers);
        sb.append("\nInstructions:\n")
                .append("- Return exactly ").append(dayCount).append(" days, numbered 1 to ")
                .append(dayCount).append(".\n")
                .append("- Balance the group: every member should have something they're excited about ")
                .append("across the trip. Respect every 'avoid' item strictly (dietary, mobility, etc.).\n")
                .append("- Match the number of activities per day to the group's pace ")
                .append("(Relaxed ~3, Balanced ~4-5, Packed ~6+). Include meals.\n")
                .append("- Keep estimatedCost per person in US dollars, consistent with the group's budgets ")
                .append("(Low < $50/day, Medium $50-150/day, High > $150/day). Use 0 for free activities.\n")
                .append("- time is the start time in 24-hour HH:mm. Order activities chronologically.\n")
                .append("- placeName must be a real, specific, searchable place in ").append(destination)
                .append(" (e.g. 'Art Institute of Chicago'), not a generic description.\n")
                .append("- description is one or two short sentences.\n")
                .append("- note is one short sentence explaining whose interests the day serves, ")
                .append("using first names, e.g. 'Museum morning for Sam, food tour for Jordan.'\n");
        return sb.toString();
    }

    public static String alternativesPrompt(String destination, String date,
                                            String targetTitle, String targetTime, String targetDescription,
                                            List<String> otherActivities, List<MemberAnswers> answers) {
        StringBuilder sb = new StringBuilder();
        sb.append("A group visiting ").append(destination).append(" wants to replace one activity ")
                .append("in their plan for ").append(date).append(".\n\n")
                .append("Activity to replace: ").append(targetTime).append(" ").append(targetTitle);
        if (targetDescription != null && !targetDescription.isEmpty()) {
            sb.append(" — ").append(targetDescription);
        }
        sb.append("\n\nThe rest of that day:\n");
        if (otherActivities.isEmpty()) {
            sb.append("- (nothing else planned)\n");
        } else {
            for (String a : otherActivities) sb.append("- ").append(a).append('\n');
        }
        sb.append("\nGroup preferences:\n");
        appendAnswers(sb, answers);
        sb.append("\nReturn exactly 3 different options that fit the same time slot (around ")
                .append(targetTime).append("), fit the group's budget and pace, don't duplicate ")
                .append("anything already planned that day, and respect every 'avoid' item. ")
                .append("time is 24-hour HH:mm, estimatedCost is per person in US dollars, and ")
                .append("placeName must be a real, specific, searchable place in ").append(destination)
                .append(".\n");
        return sb.toString();
    }

    private static void appendAnswers(StringBuilder sb, List<MemberAnswers> answers) {
        if (answers.isEmpty()) {
            sb.append("- (no one has answered yet; plan a well-rounded trip)\n");
            return;
        }
        for (MemberAnswers a : answers) {
            sb.append("- ").append(a.name).append(": budget ").append(orDash(a.budget))
                    .append(", pace ").append(orDash(a.pace))
                    .append(", interests ").append(a.interests == null || a.interests.isEmpty()
                            ? "-" : String.join(", ", a.interests));
            if (a.mustDos != null && !a.mustDos.trim().isEmpty()) {
                sb.append(", must-dos: ").append(a.mustDos.trim());
            }
            if (a.avoid != null && !a.avoid.trim().isEmpty()) {
                sb.append(", avoid: ").append(a.avoid.trim());
            }
            sb.append('\n');
        }
    }

    private static String orDash(String s) {
        return s == null || s.isEmpty() ? "-" : s;
    }
}
