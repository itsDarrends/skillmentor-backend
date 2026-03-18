package com.stemlink.skillmentor.controllers;

import com.stemlink.skillmentor.dto.SessionDTO;
import com.stemlink.skillmentor.dto.response.SessionResponseDTO;
import com.stemlink.skillmentor.entities.Session;
import com.stemlink.skillmentor.exceptions.SkillMentorException;
import com.stemlink.skillmentor.security.UserPrincipal;
import com.stemlink.skillmentor.services.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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
                .stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @GetMapping("{id}")
    public ResponseEntity<SessionResponseDTO> getSessionById(@PathVariable Long id) {
        return sendOkResponse(toDTO(sessionService.getSessionById(id)));
    }

    @PostMapping
    public ResponseEntity<SessionResponseDTO> createSession(@Valid @RequestBody SessionDTO sessionDTO) {
        return sendCreatedResponse(toDTO(sessionService.createNewSession(sessionDTO)));
    }

    @PutMapping("{id}")
    public ResponseEntity<SessionResponseDTO> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody SessionDTO updatedSessionDTO) {
        return sendOkResponse(toDTO(sessionService.updateSessionById(id, updatedSessionDTO)));
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
        return sendCreatedResponse(toDTO(session));
    }

    @GetMapping("/my-sessions")
    public ResponseEntity<List<SessionResponseDTO>> getMySessions(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<Session> sessions = sessionService.getSessionsByStudentEmail(userPrincipal.getEmail());
        return sendOkResponse(sessions.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @PatchMapping("/{id}/review")
    public ResponseEntity<SessionResponseDTO> submitReview(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Session session = sessionService.getSessionById(id);
        if (!session.getStudent().getEmail().equals(userPrincipal.getEmail())) {
            throw new SkillMentorException("Unauthorized", HttpStatus.FORBIDDEN);
        }
        if (body.get("review") != null) session.setStudentReview((String) body.get("review"));
        if (body.get("rating") != null) session.setStudentRating((Integer) body.get("rating"));
        return sendOkResponse(toDTO(sessionService.saveSession(session)));
    }

    private SessionResponseDTO toDTO(Session session) {
        SessionResponseDTO dto = new SessionResponseDTO();
        dto.setId(session.getId());
        dto.setMentorName(session.getMentor().getFirstName() + " " + session.getMentor().getLastName());
        dto.setMentorProfileImageUrl(session.getMentor().getProfileImageUrl());
        dto.setSubjectName(session.getSubject().getSubjectName());
        dto.setSessionAt(session.getSessionAt());
        dto.setDurationMinutes(session.getDurationMinutes());
        dto.setSessionStatus(session.getSessionStatus());
        dto.setPaymentStatus(session.getPaymentStatus());
        dto.setMeetingLink(session.getMeetingLink());
        dto.setStudentReview(session.getStudentReview());
        dto.setStudentRating(session.getStudentRating());
        return dto;
    }
}