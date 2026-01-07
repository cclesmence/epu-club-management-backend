package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.dto.request.CampusFilterRequest;
import com.sep490.backendclubmanagement.entity.Campus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CampusRepository extends JpaRepository<Campus, Long> {
    boolean existsByCampusCode(String campusCode);
    @Query(value = """
            SELECT c.*
            FROM campuses c
            WHERE (
                   :#{#req.keyword} IS NULL
                OR LOWER(c.campus_name) LIKE LOWER(CONCAT('%', :#{#req.keyword}, '%'))
                OR LOWER(c.campus_code) LIKE LOWER(CONCAT('%', :#{#req.keyword}, '%'))
                OR LOWER(c.address)     LIKE LOWER(CONCAT('%', :#{#req.keyword}, '%'))
              )
            """,
            nativeQuery = true,
            countProjection = "c.id")
    Page<Campus> getAllByFilter(@Param("req") CampusFilterRequest req, Pageable pageable);
}


