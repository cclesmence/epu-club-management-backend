package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.AdminDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminDepartmentRepository extends JpaRepository<AdminDepartment, Long> {

    @Query("""
           select d from AdminDepartment d
           join fetch d.campus c
           where d.id = :id
           """)
    Optional<AdminDepartment> findByIdWithCampus(@Param("id") Long id);
}

