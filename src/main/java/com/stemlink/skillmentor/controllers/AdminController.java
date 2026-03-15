package com.stemlink.skillmentor.controllers;

import com.stemlink.skillmentor.entities.Session;
import com.stemlink.skillmentor.respositories.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController extends AbstractController {

    private final SessionRepository sessionRepository;

    @GetMapping("/sessions")
    public ResponseEntity<List<Session>> getAllSessions() {
        return sendOkResponse(sessionRepository.findAll());
    }

    @PatchMapping("/sessions/{id}/status")
    public ResponseEntity<Session> updateSessionStatus(
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
        return sendOkResponse(sessionRepository.save(session));
    }

    @PatchMapping("/sessions/{id}/meeting-link")
    public ResponseEntity<Session> updateMeetingLink(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        Session session = sessionRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setMeetingLink(body.get("meetingLink"));
        return sendOkResponse(sessionRepository.save(session));
    }
}