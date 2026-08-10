package com.nexacore.systemmodule.backup.repository;

import com.nexacore.systemmodule.backup.entity.SysBackupDatabaseResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackupDatabaseResultRepository extends JpaRepository<SysBackupDatabaseResult, Long> {}
