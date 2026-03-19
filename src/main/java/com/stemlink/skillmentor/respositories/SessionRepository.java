package com.stemlink.skillmentor.respositories;

import com.stemlink.skillmentor.entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session,Long> {
    List<Session> findByStudent_Email(String email);
    List<Session> findByMentor_Id(Long mentorId);
    List<Session> findBySubject_Id(Long subjectId);
}