package com.nexacore.systemmodule.privilege.catalog.repository;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActionRepository extends JpaRepository<SysPrivAction, Long> {
    List<SysPrivAction> findAllByActiveTrueOrderByDisplayOrderAscActionCodeAsc();

    Optional<SysPrivAction> findByActionCode(String actionCode);
}
