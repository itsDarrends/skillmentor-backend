package com.stemlink.skillmentor.controllers;

import com.stemlink.skillmentor.dto.MentorDTO;
import com.stemlink.skillmentor.entities.Mentor;
import com.stemlink.skillmentor.entities.Session;
import com.stemlink.skillmentor.security.UserPrincipal;
import com.stemlink.skillmentor.services.MentorService;
import com.stemlink.skillmentor.respositories.SessionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.stemlink.skillmentor.constants.UserRoles.*;

@RestController
@RequestMapping(path = "/api/v1/mentors")
@RequiredArgsConstructor
@Validated
public class MentorController extends AbstractController {

    private final MentorService mentorService;
    private final ModelMapper modelMapper;
    private final SessionRepository sessionRepository;

    @GetMapping
    public ResponseEntity<Page<Mentor>> getAllMentors(
            @RequestParam(required = false) String name,
            Pageable pageable) {
        return sendOkResponse(mentorService.getAllMentors(name, pageable));
    }

    @GetMapping("{id}")
    public ResponseEntity<Map<String, Object>> getMentorById(@PathVariable Long id) {
        Mentor mentor = mentorService.getMentorById(id);
        List<Session> sessions = sessionRepository.findByMentor_Id(id);

        long totalStudents = sessions.stream()
                .map(s -> s.getStudent().getId()).distinct().count();
        double avgRating = sessions.stream()
                .filter(s -> s.getStudentRating() != null)
                .mapToInt(Session::getStudentRating)
                .average().orElse(0.0);
        long totalReviews = sessions.stream()
                .filter(s -> s.getStudentRating() != null).count();

        List<Map<String, Object>> reviews = sessions.stream()
                .filter(s -> s.getStudentReview() != null && !s.getStudentReview().isEmpty())
                .map(s -> {
                    Map<String, Object> r = new HashMap<>();
                    r.put("studentName", s.getStudent().getFirstName() + " " + s.getStudent().getLastName());
                    r.put("review", s.getStudentReview());
                    r.put("rating", s.getStudentRating());
                    r.put("date", s.getUpdatedAt());
                    return r;
                }).toList();

        // Per-subject enrollment counts
        Map<Long, Long> subjectEnrollments = sessions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getSubject().getId(),
                        Collectors.counting()
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("mentor", mentor);
        response.put("totalStudentsTaught", totalStudents);
        response.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
        response.put("totalReviews", totalReviews);
        response.put("reviews", reviews);
        response.put("subjectEnrollments", subjectEnrollments);
        return sendOkResponse(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + ROLE_ADMIN + "', '" + ROLE_MENTOR + "')")
    public ResponseEntity<Mentor> createMentor(
            @Valid @RequestBody MentorDTO mentorDTO,
            Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Mentor mentor = modelMapper.map(mentorDTO, Mentor.class);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            mentor.setMentorId(userPrincipal.getId());
            mentor.setFirstName(userPrincipal.getFirstName());
            mentor.setLastName(userPrincipal.getLastName());
            mentor.setEmail(userPrincipal.getEmail());
        }
        return sendCreatedResponse(mentorService.createNewMentor(mentor));
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAnyRole('" + ROLE_ADMIN + "', '" + ROLE_MENTOR + "')")
    public ResponseEntity<Mentor> updateMentor(
            @PathVariable Long id,
            @Valid @RequestBody MentorDTO updatedMentorDTO) {
        Mentor mentor = modelMapper.map(updatedMentorDTO, Mentor.class);
        return sendOkResponse(mentorService.updateMentorById(id, mentor));
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAnyRole('" + ROLE_ADMIN + "')")
    public ResponseEntity<Mentor> deleteMentor(@PathVariable Long id) {
        mentorService.deleteMentor(id);
        return sendNoContentResponse();
    }
}