package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByTeacherId(String teacherId);
    long count();

    @Query("SELECT t FROM Teacher t JOIN t.subjects s WHERE s.id = :subjectId")
    List<Teacher> findBySubjectId(@Param("subjectId") Long subjectId);

    @Query("SELECT t FROM Teacher t JOIN t.subjects s WHERE s.id = :subjectId")
    Page<Teacher> findBySubjectId(@Param("subjectId") Long subjectId, Pageable pageable);

    @Query("SELECT t FROM Teacher t JOIN t.classrooms c WHERE c.id = :classroomId")
    List<Teacher> findByClassroomId(@Param("classroomId") Long classroomId);

    @Query("SELECT t FROM Teacher t JOIN t.classrooms c WHERE c.id = :classroomId")
    Page<Teacher> findByClassroomId(@Param("classroomId") Long classroomId, Pageable pageable);

    // New methods for filtering and pagination
    @Query("SELECT t FROM Teacher t WHERE " +
            "(:firstName IS NULL OR t.firstName LIKE %:firstName%) AND " +
            "(:lastName IS NULL OR t.lastName LIKE %:lastName%) AND " +
            "(:subjectId IS NULL OR EXISTS (SELECT s FROM t.subjects s WHERE s.id = :subjectId))")
    Page<Teacher> findByFilters(@Param("firstName") String firstName,
                                @Param("lastName") String lastName,
                                @Param("subjectId") Long subjectId,
                                Pageable pageable);
}