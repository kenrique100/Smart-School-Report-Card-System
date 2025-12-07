package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.StudentSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentSubjectRepository extends JpaRepository<StudentSubject, Long> {

    @Query("SELECT ss FROM StudentSubject ss WHERE ss.student.id = :studentId")
    List<StudentSubject> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT ss FROM StudentSubject ss WHERE ss.student.id = :studentId AND ss.subject.id = :subjectId")
    Optional<StudentSubject> findByStudentIdAndSubjectId(@Param("studentId") Long studentId,
                                                         @Param("subjectId") Long subjectId);

    @Query("SELECT ss FROM StudentSubject ss WHERE ss.subject.id = :subjectId")
    List<StudentSubject> findBySubjectId(@Param("subjectId") Long subjectId);

    @Query("SELECT ss FROM StudentSubject ss WHERE ss.student.id = :studentId ORDER BY ss.subject.name")
    List<StudentSubject> findByStudentIdOrderBySubjectName(@Param("studentId") Long studentId);

    @Query("SELECT ss FROM StudentSubject ss JOIN FETCH ss.subject WHERE ss.student.id = :studentId")
    List<StudentSubject> findByStudentIdWithSubject(@Param("studentId") Long studentId);

    @Query("SELECT ss FROM StudentSubject ss WHERE ss.student.id = :studentId AND ss.isCompulsory = true")
    List<StudentSubject> findCompulsoryByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT ss FROM StudentSubject ss WHERE ss.student.id = :studentId AND ss.isCompulsory = false")
    List<StudentSubject> findOptionalByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(ss) FROM StudentSubject ss WHERE ss.student.id = :studentId")
    long countByStudentId(@Param("studentId") Long studentId);

    @Modifying
    @Query("DELETE FROM StudentSubject ss WHERE ss.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    @Modifying
    @Query("DELETE FROM StudentSubject ss WHERE ss.student.id = :studentId AND ss.subject.id = :subjectId")
    void deleteByStudentIdAndSubjectId(@Param("studentId") Long studentId,
                                       @Param("subjectId") Long subjectId);

    @Query("SELECT ss FROM StudentSubject ss WHERE ss.student.id = :studentId AND ss.subject.department.id = :departmentId")
    List<StudentSubject> findByStudentIdAndDepartmentId(@Param("studentId") Long studentId,
                                                        @Param("departmentId") Long departmentId);

    @Query("SELECT DISTINCT ss.subject.id FROM StudentSubject ss WHERE ss.student.id = :studentId")
    List<Long> findSubjectIdsByStudentId(@Param("studentId") Long studentId);

    boolean existsByStudent_IdAndSubject_Id(Long studentId, Long subjectId);

    long countByStudent_Id(Long studentId);

    @Query("SELECT ss FROM StudentSubject ss WHERE ss.subject.id = :subjectId")
    List<StudentSubject> findBySubject_Id(@Param("subjectId") Long subjectId);

    @Query("SELECT ss FROM StudentSubject ss WHERE ss.student.id = :studentId")
    List<StudentSubject> findByStudent_Id(@Param("studentId") Long studentId);
}