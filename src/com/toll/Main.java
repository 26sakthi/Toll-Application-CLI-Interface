package com.toll;

import com.toll.app.TollManagementApp;
import com.toll.context.TollSystemBootstrap;
import com.toll.context.TollSystemContext;

/**
 * Main application entry point for the Toll Management System (FASTag).
 */
public class Main {
    public static void main(String[] args) {
        TollSystemContext context = TollSystemBootstrap.initializeSystem();
        TollManagementApp app = new TollManagementApp(context);
        app.start();
    }
}
