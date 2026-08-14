package com.nexacore.kycmodule.person.controller;

import com.nexacore.commonmodule.dto.SearchDto;
import com.nexacore.kycmodule.person.service.implementations.PersonService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PersonControllerTest {

    @Test
    void searchSortsOnlyByPersonProfileProperties() {
        PersonService service = mock(PersonService.class);
        PersonController controller = new PersonController(service);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        controller.search(new SearchDto("", 0, 10, null, "NEXACORE_APP"));

        verify(service).search(org.mockito.ArgumentMatchers.eq(""), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull()
                .extracting(Sort.Order::getDirection).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id")).isNotNull()
                .extracting(Sort.Order::getDirection).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("person.firstName")).isNull();
    }
}
