package com.nexacore.systemmodule.workflow.engine.local;

import com.nexacore.gatewaymodule.privilege.service.interfaces.PrivilegeModuleGateway;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowActionRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDetailRequestDto;
import com.nexacore.systemmodule.accesscontrol.security.*;
import com.nexacore.systemmodule.workflow.definition.repository.*;
import com.nexacore.systemmodule.workflow.execution.repository.*;
import com.nexacore.systemmodule.workflow.service.implementations.WorkflowHistoryServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkflowObjectAuthorizationTest {

    private final WorkflowInstanceRepository instanceRepository = mock(WorkflowInstanceRepository.class);
    private final WorkflowTaskRepository taskRepository = mock(WorkflowTaskRepository.class);
    private final WorkflowHistoryRepository historyRepository = mock(WorkflowHistoryRepository.class);
    private final DataScopeService dataScopeService = new DataScopeService();
    private final LocalWorkflowEngine engine = new LocalWorkflowEngine(
            mock(WorkflowDefinitionRepository.class), mock(WorkflowVersionRepository.class),
            mock(WorkflowStepRepository.class), mock(WorkflowTransitionRepository.class),
            mock(WorkflowAssignmentPolicyRepository.class), instanceRepository, taskRepository,
            historyRepository, mock(PrivilegeModuleGateway.class), dataScopeService);
    private final WorkflowHistoryServiceImpl historyService =
            new WorkflowHistoryServiceImpl(instanceRepository, historyRepository, dataScopeService);

    @BeforeEach
    void authenticateForTenantA() {
        AuthenticatedRequestContextHolder.set(new AuthenticatedRequestContext(
                7L, "tenant-a-user", 3L, "web",
                Set.of(new UserScopeAssignment(100L, null, null)),
                "trace", Set.of()));
        // Tenant-B object IDs are absent from repository queries constrained to tenant A.
        when(taskRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        when(instanceRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
    }

    @AfterEach
    void clearContext() {
        AuthenticatedRequestContextHolder.clear();
    }

    @Test
    void tenantACannotReadOrActOnTenantBTask() {
        assertThatThrownBy(() -> engine.getTask(new WorkflowTaskDetailRequestDto(5002L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workflow task not found");
        assertThatThrownBy(() -> engine.completeTask(WorkflowActionRequestDto.builder()
                        .workflowTaskId(5002L).actionCode("APPROVE").actorUserId(7L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workflow task not found");

        verify(taskRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void tenantACannotReadTenantBInstanceOrHistory() {
        assertThatThrownBy(() -> engine.getInstance(WorkflowInstanceRequestDto.builder()
                        .workflowInstanceId(6002L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workflow instance not found");
        assertThatThrownBy(() -> historyService.getHistory(6002L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workflow instance not found");

        verifyNoInteractions(historyRepository);
    }
}
