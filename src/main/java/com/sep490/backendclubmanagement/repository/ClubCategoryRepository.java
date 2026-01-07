package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.club.ClubCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClubCategoryRepository extends JpaRepository<ClubCategory, Long> {

    @Query("SELECT cc FROM ClubCategory cc WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(cc.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ClubCategory> findAllWithFilter(@Param("keyword") String keyword, Pageable pageable);

    Optional<ClubCategory> findByCategoryNameIgnoreCase(String categoryName);

    boolean existsByCategoryNameIgnoreCase(String categoryName);

    @Query("SELECT CASE WHEN COUNT(cc) > 0 THEN true ELSE false END FROM ClubCategory cc " +
           "WHERE LOWER(cc.categoryName) = LOWER(:categoryName) AND cc.id != :id")
    boolean existsByCategoryNameIgnoreCaseAndIdNot(@Param("categoryName") String categoryName, @Param("id") Long id);
}

