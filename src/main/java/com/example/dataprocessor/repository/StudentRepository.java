package com.example.dataprocessor.repository;

import com.example.dataprocessor.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    @Query("SELECT s FROM Student s WHERE " +
           "(:studentId IS NULL OR s.studentId = :studentId) AND " +
           "(:clazz IS NULL OR s.clazz = :clazz)")
    Page<Student> findByFilters(@Param("studentId") Long studentId, 
                               @Param("clazz") String clazz, 
                               Pageable pageable);
}
