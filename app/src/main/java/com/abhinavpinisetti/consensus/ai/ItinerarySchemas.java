package com.abhinavpinisetti.consensus.ai;

import com.google.firebase.ai.type.Schema;

import java.util.LinkedHashMap;
import java.util.Map;

/** JSON schemas passed to Gemini so responses always have the shape {@link ItineraryParser} expects. */
public final class ItinerarySchemas {

    private ItinerarySchemas() {}

    /** { title, time (HH:mm), description, estimatedCost (number), placeName } */
    public static Schema activity() {
        Map<String, Schema> props = new LinkedHashMap<>();
        props.put("title", Schema.str("Short activity name"));
        props.put("time", Schema.str("Start time in 24-hour HH:mm format"));
        props.put("description", Schema.str("One or two short sentences"));
        props.put("estimatedCost", Schema.numDouble("Estimated cost per person in US dollars; 0 if free"));
        props.put("placeName", Schema.str("Real, specific, searchable place name"));
        return Schema.obj(props);
    }

    /** { days: [ { dayNumber, note, activities: [activity] } ] } */
    public static Schema plan() {
        Map<String, Schema> dayProps = new LinkedHashMap<>();
        dayProps.put("dayNumber", Schema.numInt("1-based day of the trip"));
        dayProps.put("note", Schema.str("One sentence on whose interests this day serves"));
        dayProps.put("activities", Schema.array(activity()));

        Map<String, Schema> props = new LinkedHashMap<>();
        props.put("days", Schema.array(Schema.obj(dayProps)));
        return Schema.obj(props);
    }

    /** { options: [activity, activity, activity] } */
    public static Schema alternatives() {
        Map<String, Schema> props = new LinkedHashMap<>();
        props.put("options", Schema.array(activity()));
        return Schema.obj(props);
    }
}
