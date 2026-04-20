package com.stemlink.skillmentor.impl;

import com.stemlink.skillmentor.dto.SessionDTO;
import com.stemlink.skillmentor.entities.Mentor;
import com.stemlink.skillmentor.entities.Session;
import com.stemlink.skillmentor.entities.Student;
import com.stemlink.skillmentor.entities.Subject;
import com.stemlink.skillmentor.exceptions.SkillMentorException;
import com.stemlink.skillmentor.respositories.MentorRepository;
import com.stemlink.skillmentor.respositories.SessionRepository;
import com.stemlink.skillmentor.respositories.StudentRepository;
import com.stemlink.skillmentor.respositories.SubjectRepository;
import com.stemlink.skillmentor.security.UserPrincipal;
import com.stemlink.skillmentor.services.impl.SessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionServiceImpl Tests")
class SessionServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private MentorRepository mentorRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private SessionServiceImpl sessionService;

    private UserPrincipal userPrincipal;
    private SessionDTO sessionDTO;
    private Student student;
    private Mentor mentor;
    private Subject subject;

    @BeforeEach
    void setUp() {
        // Setup user principal
        userPrincipal = new UserPrincipal(
                "user_abc123",
                "darren@gmail.com",
                "Darren",
                "De Silva"
        );

        // Setup mentor
        mentor = new Mentor();
        mentor.setId(1L);
        mentor.setFirstName("Michelle");
        mentor.setLastName("Burns");
        mentor.setEmail("michelle@example.com");
        mentor.setSessions(new ArrayList<>());

        // Setup subject
        subject = new Subject();
        subject.setId(1L);
        subject.setSubjectName("AWS Developer Associate");
        subject.setMentor(mentor);

        // Setup student
        student = new Student();
        student.setId(1);
        student.setStudentId("user_abc123");
        student.setEmail("darren@gmail.com");
        student.setFirstName("Darren");
        student.setLastName("De Silva");
        student.setSessions(new ArrayList<>());

        // Setup session DTO
        sessionDTO = new SessionDTO();
        sessionDTO.setMentorId(1L);
        sessionDTO.setSubjectId(1L);
        sessionDTO.setDurationMinutes(60);
        // Future date
        Date futureDate = new Date(System.currentTimeMillis() + 86400000); // tomorrow
        sessionDTO.setSessionAt(futureDate);
    }


    @Test
    @DisplayName("Should successfully enroll a new student in a session")
    void enrollSession_Success_NewStudent() {
        // Arrange
        when(studentRepository.findByEmail("darren@gmail.com"))
                .thenReturn(Optional.empty()); // student doesn't exist yet
        when(studentRepository.save(any(Student.class)))
                .thenReturn(student);
        when(sessionRepository.findByStudent_Email("darren@gmail.com"))
                .thenReturn(new ArrayList<>()); // no existing sessions
        when(mentorRepository.findById(1L))
                .thenReturn(Optional.of(mentor));
        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));
        when(sessionRepository.save(any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Session result = sessionService.enrollSession(userPrincipal, sessionDTO);

        // Assert
        assertNotNull(result);
        assertEquals("scheduled", result.getSessionStatus());
        assertEquals("pending", result.getPaymentStatus());
        assertEquals(60, result.getDurationMinutes());
        verify(sessionRepository, times(1)).save(any(Session.class));
    }

    @Test
    @DisplayName("Should reuse existing student when enrolling")
    void enrollSession_Success_ExistingStudent() {
        // Arrange
        when(studentRepository.findByEmail("darren@gmail.com"))
                .thenReturn(Optional.of(student)); // student already exists
        when(sessionRepository.findByStudent_Email("darren@gmail.com"))
                .thenReturn(new ArrayList<>());
        when(mentorRepository.findById(1L))
                .thenReturn(Optional.of(mentor));
        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));
        when(sessionRepository.save(any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Session result = sessionService.enrollSession(userPrincipal, sessionDTO);

        // Assert
        assertNotNull(result);
        // student was NOT created again
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("Should throw CONFLICT when student already has active booking")
    void enrollSession_Fail_DoubleBooking() {
        // Arrange
        Session existingSession = new Session();
        existingSession.setMentor(mentor);
        existingSession.setSubject(subject);
        existingSession.setSessionStatus("scheduled"); // active session

        when(studentRepository.findByEmail("darren@gmail.com"))
                .thenReturn(Optional.of(student));
        when(sessionRepository.findByStudent_Email("darren@gmail.com"))
                .thenReturn(List.of(existingSession));

        // Act & Assert
        SkillMentorException exception = assertThrows(
                SkillMentorException.class,
                () -> sessionService.enrollSession(userPrincipal, sessionDTO)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getMessage().contains("active booking"));
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Should allow booking if previous session is completed")
    void enrollSession_Success_PreviousSessionCompleted() {
        // Arrange
        Session completedSession = new Session();
        completedSession.setMentor(mentor);
        completedSession.setSubject(subject);
        completedSession.setSessionStatus("completed"); // completed - allowed to book again

        when(studentRepository.findByEmail("darren@gmail.com"))
                .thenReturn(Optional.of(student));
        when(sessionRepository.findByStudent_Email("darren@gmail.com"))
                .thenReturn(List.of(completedSession));
        when(mentorRepository.findById(1L))
                .thenReturn(Optional.of(mentor));
        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));
        when(sessionRepository.save(any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Session result = sessionService.enrollSession(userPrincipal, sessionDTO);

        // Assert
        assertNotNull(result);
        verify(sessionRepository, times(1)).save(any(Session.class));
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when session date is in the past")
    void enrollSession_Fail_PastDate() {
        // Arrange
        Date pastDate = new Date(System.currentTimeMillis() - 86400000); // yesterday
        sessionDTO.setSessionAt(pastDate);

        when(studentRepository.findByEmail("darren@gmail.com"))
                .thenReturn(Optional.of(student));
        when(sessionRepository.findByStudent_Email("darren@gmail.com"))
                .thenReturn(new ArrayList<>());

        // Act & Assert
        SkillMentorException exception = assertThrows(
                SkillMentorException.class,
                () -> sessionService.enrollSession(userPrincipal, sessionDTO)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("past"));
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when mentor does not exist")
    void enrollSession_Fail_MentorNotFound() {
        // Arrange
        when(studentRepository.findByEmail("darren@gmail.com"))
                .thenReturn(Optional.of(student));
        when(sessionRepository.findByStudent_Email("darren@gmail.com"))
                .thenReturn(new ArrayList<>());
        when(mentorRepository.findById(1L))
                .thenReturn(Optional.empty()); // mentor not found

        // Act & Assert
        SkillMentorException exception = assertThrows(
                SkillMentorException.class,
                () -> sessionService.enrollSession(userPrincipal, sessionDTO)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when subject does not exist")
    void enrollSession_Fail_SubjectNotFound() {
        // Arrange
        when(studentRepository.findByEmail("darren@gmail.com"))
                .thenReturn(Optional.of(student));
        when(sessionRepository.findByStudent_Email("darren@gmail.com"))
                .thenReturn(new ArrayList<>());
        when(mentorRepository.findById(1L))
                .thenReturn(Optional.of(mentor));
        when(subjectRepository.findById(1L))
                .thenReturn(Optional.empty()); // subject not found

        // Act & Assert
        SkillMentorException exception = assertThrows(
                SkillMentorException.class,
                () -> sessionService.enrollSession(userPrincipal, sessionDTO)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Should default duration to 60 minutes when not specified")
    void enrollSession_DefaultDuration() {
        // Arrange
        sessionDTO.setDurationMinutes(null); // no duration set

        when(studentRepository.findByEmail("darren@gmail.com"))
                .thenReturn(Optional.of(student));
        when(sessionRepository.findByStudent_Email("darren@gmail.com"))
                .thenReturn(new ArrayList<>());
        when(mentorRepository.findById(1L))
                .thenReturn(Optional.of(mentor));
        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));
        when(sessionRepository.save(any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Session result = sessionService.enrollSession(userPrincipal, sessionDTO);

        // Assert
        assertEquals(60, result.getDurationMinutes());
    }

    @Test
    @DisplayName("Should return sessions for a student by email")
    void getSessionsByStudentEmail_Success() {
        // Arrange
        Session session1 = new Session();
        Session session2 = new Session();
        List<Session> sessions = List.of(session1, session2);

        when(sessionRepository.findByStudent_Email("darren@gmail.com"))
                .thenReturn(sessions);

        // Act
        List<Session> result = sessionService.getSessionsByStudentEmail("darren@gmail.com");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(sessionRepository, times(1)).findByStudent_Email("darren@gmail.com");
    }

    @Test
    @DisplayName("Should return empty list when student has no sessions")
    void getSessionsByStudentEmail_Empty() {
        // Arrange
        when(sessionRepository.findByStudent_Email("darren@gmail.com"))
                .thenReturn(new ArrayList<>());

        // Act
        List<Session> result = sessionService.getSessionsByStudentEmail("darren@gmail.com");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return session when found by ID")
    void getSessionById_Success() {
        // Arrange
        Session session = new Session();
        session.setId(1);

        when(sessionRepository.findById(1L))
                .thenReturn(Optional.of(session));

        // Act
        Session result = sessionService.getSessionById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when session does not exist")
    void getSessionById_NotFound() {
        // Arrange
        when(sessionRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Act & Assert
        SkillMentorException exception = assertThrows(
                SkillMentorException.class,
                () -> sessionService.getSessionById(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("Should save and return session")
    void saveSession_Success() {
        // Arrange
        Session session = new Session();
        session.setStudentReview("Great session!");
        session.setStudentRating(5);

        when(sessionRepository.save(session)).thenReturn(session);

        // Act
        Session result = sessionService.saveSession(session);

        // Assert
        assertNotNull(result);
        assertEquals("Great session!", result.getStudentReview());
        assertEquals(5, result.getStudentRating());
        verify(sessionRepository, times(1)).save(session);
    }
}