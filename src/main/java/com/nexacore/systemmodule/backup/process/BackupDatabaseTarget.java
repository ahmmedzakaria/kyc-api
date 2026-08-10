package com.nexacore.systemmodule.backup.process;

public record BackupDatabaseTarget(String logicalName, String host, int port,
                                   String databaseName, String username, String password) {}
