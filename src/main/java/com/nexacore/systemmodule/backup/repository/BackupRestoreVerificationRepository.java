package com.nexacore.systemmodule.backup.repository;
import com.nexacore.systemmodule.backup.entity.SysBackupRestoreVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BackupRestoreVerificationRepository extends JpaRepository<SysBackupRestoreVerification,Long>{ List<SysBackupRestoreVerification> findTop100ByOrderByVerifiedAtDesc(); Optional<SysBackupRestoreVerification> findFirstByBackupJobIdOrderByVerifiedAtDesc(UUID backupJobId); }
