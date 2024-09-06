package org.hyperagents.jacamo.artifacts.utils.time;

import cartago.OpFeedbackParam;

public interface TimestampComparator<T> {
    public void isOutOfOrder(T inputTimestamp, OpFeedbackParam<Boolean> result);
}
