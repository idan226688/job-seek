package com.jobseek.repository;

import com.jobseek.model.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<JobDescription, Integer> {
}
