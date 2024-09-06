package org.hyperagents.jacamo.artifacts.utils.time;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

import java.util.Map;
import java.util.HashMap;

public class VectorClockArtifact extends Artifact implements TimestampComparator<String> {
    private Map<String, Integer> localVectorClock;

    public void init() {
        localVectorClock = new HashMap<>();
    }

    @OPERATION
    public void isEventInOrder(String eventClock, OpFeedbackParam<Boolean> result) {

    }

    @Override
    @OPERATION
    public void isOutOfOrder(String inputVectorClockStr, OpFeedbackParam<Boolean> result) {
        Map<String, Integer> inputVectorClock = parseVectorClock(inputVectorClockStr);
        boolean outOfOrder = false;

        for (Map.Entry<String, Integer> entry : inputVectorClock.entrySet()) {
            String key = entry.getKey();
            int inputTime = entry.getValue();
            int localTime = localVectorClock.getOrDefault(key, 0);

            if (inputTime < localTime) {
                outOfOrder = true;
            }
        }

        localVectorClock.putAll(inputVectorClock);

        result.set(outOfOrder);
    }

    private Map<String, Integer> parseVectorClock(String vectorClockStr) {
        Map<String, Integer> clockMap = new HashMap<>();
        vectorClockStr = vectorClockStr.replace("{", "").replace("}", "").trim();
        String[] entries = vectorClockStr.split(",");
        for (String entry : entries) {
            String[] parts = entry.split("=");
            if (parts.length == 2) {
                String key = parts[0].trim();
                int value = Integer.parseInt(parts[1].trim());
                clockMap.put(key, value);
            }
        }
        return clockMap;
    }

}
