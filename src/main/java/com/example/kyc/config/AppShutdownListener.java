package com.example.kyc.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppShutdownListener implements ServletContextListener {

    // You need a way to access your HikariDataSource instance.
    // This example assumes you're storing it statically or through some application context.
    private static HikariDataSource dataSource;

    // Setter to allow other parts of your app to register the DataSource
    public static void setDataSource(HikariDataSource ds) {
        dataSource = ds;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Optional: initialization logic if needed
        System.out.println("AppShutdownListener initialized.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("AppShutdownListener triggered: shutting down HikariCP.");
        if (dataSource != null) {
            try {
                dataSource.close();
                System.out.println("HikariDataSource closed successfully.");
            } catch (Exception e) {
                System.err.println("Error closing HikariDataSource: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("No HikariDataSource was set.");
        }
    }
}

