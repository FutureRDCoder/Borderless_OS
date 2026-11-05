package com.borderlessos.provider.repository;

import com.borderlessos.provider.entity.ProviderInquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProviderInquiryRepository
        extends JpaRepository<ProviderInquiry, UUID> {

    List<ProviderInquiry> findAllByTransferIdOrderByCreatedAtAsc(
            UUID transferId
    );
}
