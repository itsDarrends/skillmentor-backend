package com.stemlink.skillmentor.controllers;

import com.stemlink.skillmentor.dto.SessionDTO;
import com.stemlink.skillmentor.dto.response.SessionResponseDTO;
import com.stemlink.skillmentor.entities.Session;
import com.stemlink.skillmentor.security.UserPrincipal;
import com.stemlink.skillmentor.services.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api/v1/sessions")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class SessionController extends AbstractController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<List<SessionResponseDTO>> getAllSessions() {
        return sendOkResponse(sessionService.getAllSessions()
                .stream().map(this::toSessionResponseDTO).collect(Collectors.toList()));
    }

    @GetMapping("{id}")
    public ResponseEntity<SessionResponseDTO> getSessionById(@PathVariable Long id) {
        return sendOkResponse(toSessionResponseDTO(sessionService.getSessionById(id)));
    }

    @PostMapping
    public ResponseEntity<SessionResponseDTO> createSession(
            @Valid @RequestBody SessionDTO sessionDTO) {
        return sendCreatedResponse(
                toSessionResponseDTO(sessionService.createNewSession(sessionDTO)));
    }

    @PutMapping("{id}")
    public ResponseEntity<SessionResponseDTO> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody SessionDTO updatedSessionDTO) {
        return sendOkResponse(
                toSessionResponseDTO(sessionService.updateSessionById(id, updatedSessionDTO)));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return sendNoContentResponse();
    }

    @PostMapping("/enroll")
    public ResponseEntity<SessionResponseDTO> enroll(
            @RequestBody SessionDTO sessionDTO,
            Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Session session = sessionService.enrollSession(userPrincipal, sessionDTO);
        return sendCreatedResponse(toSessionResponseDTO(session));
    }

    @GetMapping("/my-sessions")
    public ResponseEntity<List<SessionResponseDTO>> getMySessions(
            Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<Session> sessions = sessionService
                .getSessionsByStudentEmail(userPrincipal.getEmail());
        return sendOkResponse(sessions.stream()
                .map(this::toSessionResponseDTO)
                .collect(Collectors.toList()));
    }

    private SessionResponseDTO toSessionResponseDTO(Session session) {
        SessionResponseDTO dto = new SessionResponseDTO();
        dto.setId(session.getId());
        dto.setMentorName(session.getMentor().getFirstName()
                + " " + session.getMentor().getLastName());
        dto.setMentorProfileImageUrl(session.getMentor().getProfileImageUrl());
        dto.setSubjectName(session.getSubject().getSubjectName());
        dto.setSessionAt(session.getSessionAt());
        dto.setDurationMinutes(session.getDurationMinutes());
        dto.setSessionStatus(session.getSessionStatus());
        dto.setPaymentStatus(session.getPaymentStatus());
        dto.setMeetingLink(session.getMeetingLink());
        return dto;
    }
}