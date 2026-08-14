package com.nexacore.systemmodule.backup.repository;
import com.nexacore.systemmodule.backup.entity.SysBackupDeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BackupDeliveryAttemptRepository extends JpaRepository<SysBackupDeliveryAttempt,Long>{ List<SysBackupDeliveryAttempt> findByBackupJobIdOrderByAttemptedAtDesc(UUID backupJobId); }
