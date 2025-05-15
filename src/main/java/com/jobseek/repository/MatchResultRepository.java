package com.jobseek.repository;

import com.jobseek.model.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MatchResultRepository extends JpaRepository<MatchResult, String> {
    List<MatchResult> findByUserIdOrderByTimestampDesc(String userId);
    Page<MatchResult> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
}

