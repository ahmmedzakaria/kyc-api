package com.nexacore.kycmodule.person.service.implementations;

import com.nexacore.kycmodule.person.dto.PersonDto;
import com.nexacore.kycmodule.person.api.KycGlobalPersonDualWriteService;
import com.nexacore.kycmodule.person.repository.*;
import com.nexacore.servicesmodule.fileservice.service.interfaces.FileManagementService;
import com.nexacore.systemmodule.accesscontrol.security.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PersonServiceObjectAuthorizationTest {

    private final PersonRepository personRepository = mock(PersonRepository.class);
    private final PersonDetailsRepository detailsRepository = mock(PersonDetailsRepository.class);
    private final PersonDocumentRepository documentRepository = mock(PersonDocumentRepository.class);
    private final PersonProfileRepository profileRepository = mock(PersonProfileRepository.class);
    private final PersonOrganizationMembershipRepository membershipRepository = mock(PersonOrganizationMembershipRepository.class);
    private final FileManagementService fileService = mock(FileManagementService.class);
    private final PersonService service = new PersonService(
            personRepository, detailsRepository, documentRepository, profileRepository,
            membershipRepository, mock(ModelMapper.class), fileService,
            mock(ApplicationEventPublisher.class), new DataScopeService(),
            mock(KycGlobalPersonDualWriteService.class));

    @BeforeEach
    void authenticateForTenantA() {
        AuthenticatedRequestContextHolder.set(new AuthenticatedRequestContext(
                7L, "tenant-a-user", 3L, "web",
                Set.of(new UserScopeAssignment(100L, 110L, 111L)),
                "trace", Set.of()));
        // A profile ID belonging only to tenant B is absent from the tenant-A scoped query.
        when(profileRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
    }

    @AfterEach
    void clearContext() {
        AuthenticatedRequestContextHolder.clear();
    }

    @Test
    void tenantACannotReadTenantBProfileOrPhoto() {
        assertThatThrownBy(() -> service.getPhoto(9002L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("KYC person profile not found");

        verifyNoInteractions(documentRepository, fileService);
    }

    @Test
    void tenantACannotUpdateOrDeleteTenantBProfile() {
        PersonDto update = PersonDto.builder().id(9002L).firstName("Unauthorized change").build();

        assertThatThrownBy(() -> service.update(update, null))
                .isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> service.delete(9002L))
                .isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(personRepository, detailsRepository, documentRepository, membershipRepository, fileService);
    }

    @Test
    void tenantACannotListOrDownloadTenantBProfileDocuments() {
        assertThatThrownBy(() -> service.getDocuments(9002L))
                .isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> service.getDocumentContent(9002L, 81L))
                .isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(documentRepository, fileService);
    }
}
