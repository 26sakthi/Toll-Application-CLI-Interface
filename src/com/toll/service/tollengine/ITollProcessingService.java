package com.toll.service.tollengine;

import com.toll.model.transaction.TollTransaction;

/**
 * Service contract for Edge Toll Processing (Module 3).
 * Controls barrier actuation, RFID matching, anti-double billing, and cash fallback diversion.
 */
public interface ITollProcessingService {

    /**
     * Executes the end-to-end crossing pipeline for a vehicle entering a plaza lane.
     */
    CrossingResponse processCrossing(CrossingRequest request);
}
