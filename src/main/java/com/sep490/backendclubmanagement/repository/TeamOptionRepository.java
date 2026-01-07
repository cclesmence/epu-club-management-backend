package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.TeamOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamOptionRepository extends JpaRepository<TeamOption, Long> {

    // Lấy danh sách team options của một recruitment với JOIN FETCH team
    @Query("SELECT DISTINCT to FROM TeamOption to " +
           "LEFT JOIN FETCH to.team " +
           "WHERE to.recruitment.id = :recruitmentId")
    List<TeamOption> findByRecruitment_Id(@Param("recruitmentId") Long recruitmentId);

    // Xóa tất cả team options của một recruitment
    void deleteByRecruitment_Id(Long recruitmentId);
}