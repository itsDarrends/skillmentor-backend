package com.stemlink.skillmentor.controllers;

import com.stemlink.skillmentor.dto.response.AdminSessionResponseDTO;
import com.stemlink.skillmentor.entities.Session;
import com.stemlink.skillmentor.respositories.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController extends AbstractController {

    private final SessionRepository sessionRepository;

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
        if (paymentStatus != null) {
            session.setPaymentStatus(paymentStatus);
        }
        String sessionStatus = body.get("sessionStatus");
        if (sessionStatus != null) {
            session.setSessionStatus(sessionStatus);
        }
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