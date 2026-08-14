package com.nexacore.systemmodule.backup.repository;

import com.nexacore.systemmodule.backup.entity.SysBackupJob;
import com.nexacore.systemmodule.backup.enums.BackupJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface BackupJobRepository extends JpaRepository<SysBackupJob, UUID> {
    @Override
    @EntityGraph(attributePaths = "databaseResults")
    Optional<SysBackupJob> findById(UUID id);

    @EntityGraph(attributePaths = "databaseResults")
    Page<SysBackupJob> findAllByOrderByRequestedAtDesc(Pageable pageable);

    boolean existsByStatusIn(Collection<BackupJobStatus> statuses);
    long countByStatusIn(Collection<BackupJobStatus> statuses);

    List<SysBackupJob> findByStatusOrderByCompletedAtDesc(BackupJobStatus status);

}
