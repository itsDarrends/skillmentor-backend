package com.stemlink.skillmentor.controllers;

import com.stemlink.skillmentor.dto.MentorDTO;
import com.stemlink.skillmentor.dto.response.AdminSessionResponseDTO;
import com.stemlink.skillmentor.entities.Mentor;
import com.stemlink.skillmentor.entities.Session;
import com.stemlink.skillmentor.entities.Student;
import com.stemlink.skillmentor.entities.Subject;
import com.stemlink.skillmentor.respositories.MentorRepository;
import com.stemlink.skillmentor.respositories.SessionRepository;
import com.stemlink.skillmentor.respositories.StudentRepository;
import com.stemlink.skillmentor.respositories.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController extends AbstractController {

    private final SessionRepository sessionRepository;
    private final MentorRepository mentorRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final ModelMapper modelMapper;

    // ─── STATISTICS ────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMentors", mentorRepository.count());
        stats.put("totalStudents", studentRepository.count());
        stats.put("totalSubjects", subjectRepository.count());
        stats.put("totalSessions", sessionRepository.count());
        stats.put("pendingSessions", sessionRepository.findAll().stream()
                .filter(s -> "pending".equals(s.getPaymentStatus())).count());
        stats.put("completedSessions", sessionRepository.findAll().stream()
                .filter(s -> "completed".equals(s.getSessionStatus())).count());
        return sendOkResponse(stats);
    }

    // ─── SESSIONS ──────────────────────────────────────────────────
    @GetMapping("/sessions")
    public ResponseEntity<List<AdminSessionResponseDTO>> getAllSessions() {
        List<Session> sessions = sessionRepository.findAll();
        List<AdminSessionResponseDTO> response = sessions.stream()
                .map(this::toAdminSessionDTO)
                .collect(Collectors.toList());
        return sendOkResponse(response);
    }

    @PatchMapping("/sessions/{id}/status")
    public ResponseEntity<AdminSessionResponseDTO> updateSessionStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        Session session = sessionRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new RuntimeException("Session not found"));
        String paymentStatus = body.get("status");
        if (paymentStatus != null) session.setPaymentStatus(paymentStatus);
        String sessionStatus = body.get("sessionStatus");
        if (sessionStatus != null) session.setSessionStatus(sessionStatus);
        return sendOkResponse(toAdminSessionDTO(sessionRepository.save(session)));
    }

    @PatchMapping("/sessions/{id}/meeting-link")
    public ResponseEntity<AdminSessionResponseDTO> updateMeetingLink(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        Session session = sessionRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setMeetingLink(body.get("meetingLink"));
        return sendOkResponse(toAdminSessionDTO(sessionRepository.save(session)));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Integer id) {
        sessionRepository.deleteById(Long.valueOf(id));
        return sendNoContentResponse();
    }

    // ─── MENTORS ───────────────────────────────────────────────────
    @GetMapping("/mentors")
    public ResponseEntity<List<Mentor>> getAllMentors() {
        return sendOkResponse(mentorRepository.findAll());
    }

    @PutMapping("/mentors/{id}")
    public ResponseEntity<Mentor> updateMentor(
            @PathVariable Long id,
            @RequestBody MentorDTO mentorDTO) {
        Mentor mentor = mentorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));
        if (mentorDTO.getFirstName() != null) mentor.setFirstName(mentorDTO.getFirstName());
        if (mentorDTO.getLastName() != null) mentor.setLastName(mentorDTO.getLastName());
        if (mentorDTO.getEmail() != null) mentor.setEmail(mentorDTO.getEmail());
        if (mentorDTO.getPhoneNumber() != null) mentor.setPhoneNumber(mentorDTO.getPhoneNumber());
        if (mentorDTO.getTitle() != null) mentor.setTitle(mentorDTO.getTitle());
        if (mentorDTO.getProfession() != null) mentor.setProfession(mentorDTO.getProfession());
        if (mentorDTO.getCompany() != null) mentor.setCompany(mentorDTO.getCompany());
        if (mentorDTO.getBio() != null) mentor.setBio(mentorDTO.getBio());
        if (mentorDTO.getProfileImageUrl() != null) mentor.setProfileImageUrl(mentorDTO.getProfileImageUrl());
        if (mentorDTO.getPositiveReviews() != null) mentor.setPositiveReviews(mentorDTO.getPositiveReviews());
        if (mentorDTO.getTotalEnrollments() != null) mentor.setTotalEnrollments(mentorDTO.getTotalEnrollments());
        if (mentorDTO.getIsCertified() != null) mentor.setIsCertified(mentorDTO.getIsCertified());
        if (mentorDTO.getStartYear() != null) mentor.setStartYear(mentorDTO.getStartYear());
        mentor.setExperienceYears(mentorDTO.getExperienceYears());
        return sendOkResponse(mentorRepository.save(mentor));
    }

    @DeleteMapping("/mentors/{id}")
    public ResponseEntity<Void> deleteMentor(@PathVariable Long id) {
        mentorRepository.deleteById(id);
        return sendNoContentResponse();
    }

    // ─── SUBJECTS ──────────────────────────────────────────────────
    @GetMapping("/subjects")
    public ResponseEntity<List<Subject>> getAllSubjects() {
        return sendOkResponse(subjectRepository.findAll());
    }

    @PutMapping("/subjects/{id}")
    public ResponseEntity<Subject> updateSubject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        if (body.get("subjectName") != null) subject.setSubjectName(body.get("subjectName"));
        if (body.get("description") != null) subject.setDescription(body.get("description"));
        if (body.get("courseImageUrl") != null) subject.setCourseImageUrl(body.get("courseImageUrl"));
        return sendOkResponse(subjectRepository.save(subject));
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        subjectRepository.deleteById(id);
        return sendNoContentResponse();
    }

    // ─── STUDENTS ──────────────────────────────────────────────────
    @GetMapping("/students")
    public ResponseEntity<List<Student>> getAllStudents() {
        return sendOkResponse(studentRepository.findAll());
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Integer id) {
        studentRepository.deleteById(id);
        return sendNoContentResponse();
    }

    // ─── HELPER ────────────────────────────────────────────────────
    private AdminSessionResponseDTO toAdminSessionDTO(Session session) {
        AdminSessionResponseDTO dto = new AdminSessionResponseDTO();
        dto.setId(session.getId());
        dto.setSessionAt(session.getSessionAt());
        dto.setDurationMinutes(session.getDurationMinutes());
        dto.setSessionStatus(session.getSessionStatus());
        dto.setPaymentStatus(session.getPaymentStatus());
        dto.setMeetingLink(session.getMeetingLink());
        if (session.getStudent() != null) {
            dto.setStudentName(session.getStudent().getFirstName()
                    + " " + session.getStudent().getLastName());
            dto.setStudentEmail(session.getStudent().getEmail());
        }
        if (session.getMentor() != null) {
            dto.setMentorName(session.getMentor().getFirstName()
                    + " " + session.getMentor().getLastName());
        }
        if (session.getSubject() != null) {
            dto.setSubjectName(session.getSubject().getSubjectName());
        }
        return dto;
    }
}