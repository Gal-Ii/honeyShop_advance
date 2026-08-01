package app.service;

import app.model.entity.user.Country;
import app.model.entity.user.Gender;
import app.model.entity.user.User;
import app.model.entity.user.UserRole;
import app.model.entity.user.UserProperties;
import app.repository.user.UserRepository;
import app.web.dto.user.RegisterRequest;
import app.web.dto.user.UpdateProfileRequest;
import app.exception.UserAlreadyExistsException;
import app.exception.InvalidUserDataException;
import app.exception.UnauthorizedActionException;
import app.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                passwordEncoder
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserShouldReturnAuthenticatedUser() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "user@example.com",
                        "password",
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        User existingUser = User.builder()
                .email("user@example.com")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        when(userRepository.findUserByEmail("user@example.com"))
                .thenReturn(Optional.of(existingUser));

        User result = userService.getCurrentUser();

        assertSame(existingUser, result);
        assertEquals("user@example.com", result.getEmail());

        verify(userRepository)
                .findUserByEmail("user@example.com");
        verifyNoInteractions(passwordEncoder);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void registerShouldSaveMaleUserWhenRequestIsValid() {
        RegisterRequest request = RegisterRequest.builder()
                .name(" Ivan Ivanov ")
                .email("  IVAN@EXAMPLE.COM  ")
                .password("password123")
                .country(Country.BULGARIA)
                .gender(Gender.MALE)
                .build();

        when(userRepository.findUserByEmail("ivan@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(request);

        assertNotNull(result);
        assertEquals("Ivan Ivanov", result.getName());
        assertEquals("ivan@example.com", result.getEmail());
        assertEquals("encoded-password", result.getPassword());
        assertEquals(Country.BULGARIA, result.getCountry());
        assertEquals(Gender.MALE, result.getGender());
        assertEquals(UserRole.USER, result.getRole());
        assertTrue(result.getIsActive());
        assertEquals(
                "/images/default-avatar-man.png",
                result.getProfilePicture()
        );
        assertNotNull(result.getCreatedOn());
        assertNotNull(result.getUpdatedOn());

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository)
                .findUserByEmail("ivan@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(userCaptor.capture());

        assertSame(result, userCaptor.getValue());

        verifyNoMoreInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void registerShouldThrowExceptionWhenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Ivan Ivanov")
                .email("  IVAN@EXAMPLE.COM  ")
                .password("password123")
                .country(Country.BULGARIA)
                .gender(Gender.MALE)
                .build();

        User existingUser = User.builder()
                .email("ivan@example.com")
                .build();

        when(userRepository.findUserByEmail("ivan@example.com"))
                .thenReturn(Optional.of(existingUser));

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.register(request)
        );

        assertEquals(
                "User with [  IVAN@EXAMPLE.COM  ] email already exists.",
                exception.getMessage()
        );

        verify(userRepository)
                .findUserByEmail("ivan@example.com");

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void registerShouldUseFemaleAvatarWhenGenderIsFemale() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Maria Ivanova")
                .email("maria@example.com")
                .password("password123")
                .country(Country.BULGARIA)
                .gender(Gender.FEMALE)
                .build();

        when(userRepository.findUserByEmail("maria@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(request);

        assertEquals(Gender.FEMALE, result.getGender());
        assertEquals(
                "/images/default-avatar-woman.png",
                result.getProfilePicture()
        );

        verify(userRepository)
                .findUserByEmail("maria@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));

        verifyNoMoreInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void ensureDefaultAdminShouldThrowExceptionWhenPropertiesAreNull() {
        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.ensureDefaultAdmin(null)
        );

        assertEquals(
                "Default user properties are required.",
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @ParameterizedTest
    @MethodSource("invalidDefaultAdminProperties")
    void ensureDefaultAdminShouldRejectInvalidProperties(
            String name,
            String email,
            String password,
            Country country,
            String expectedMessage) {

        UserProperties.DefaultUser defaultUser = new UserProperties.DefaultUser();
        defaultUser.setName(name);
        defaultUser.setEmail(email);
        defaultUser.setPassword(password);
        defaultUser.setCountry(country);

        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.ensureDefaultAdmin(defaultUser)
        );

        assertEquals(expectedMessage, exception.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    private static Stream<Arguments> invalidDefaultAdminProperties() {
        return Stream.of(
                Arguments.of(
                        (String) null,
                        "admin@example.com",
                        "password123",
                        Country.BULGARIA,
                        "User name is required."
                ),
                Arguments.of(
                        "Admin User",
                        "invalid-email",
                        "password123",
                        Country.BULGARIA,
                        "Please enter a valid email address."
                ),
                Arguments.of(
                        "Admin User",
                        "admin@example.com",
                        (String) null,
                        Country.BULGARIA,
                        "Password is required."
                ),
                Arguments.of(
                        "Admin User",
                        "admin@example.com",
                        "password123",
                        (Country) null,
                        "Country is required."
                )
        );
    }

    @Test
    void ensureDefaultAdminShouldCreateAdminWhenUserDoesNotExist() {
        UserProperties.DefaultUser defaultUser =
                new UserProperties.DefaultUser();

        defaultUser.setName("Main Admin");
        defaultUser.setEmail("  ADMIN@EXAMPLE.COM  ");
        defaultUser.setPassword("admin-password");
        defaultUser.setCountry(Country.BULGARIA);

        when(userRepository.findUserByEmail("admin@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("admin-password"))
                .thenReturn("encoded-admin-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.ensureDefaultAdmin(defaultUser);

        assertNotNull(result);
        assertEquals("Main Admin", result.getName());
        assertEquals("admin@example.com", result.getEmail());
        assertEquals(
                "encoded-admin-password",
                result.getPassword()
        );
        assertEquals(Country.BULGARIA, result.getCountry());
        assertEquals(Gender.FEMALE, result.getGender());
        assertEquals(UserRole.ADMIN, result.getRole());
        assertTrue(result.getIsActive());
        assertEquals(
                "/images/default-avatar-woman.png",
                result.getProfilePicture()
        );
        assertNotNull(result.getCreatedOn());
        assertNotNull(result.getUpdatedOn());

        verify(userRepository)
                .findUserByEmail("admin@example.com");
        verify(passwordEncoder)
                .encode("admin-password");
        verify(userRepository).save(result);

        verifyNoMoreInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void ensureDefaultAdminShouldRestoreExistingInactiveUser() {
        UserProperties.DefaultUser defaultUser =
                new UserProperties.DefaultUser();

        defaultUser.setName("Main Admin");
        defaultUser.setEmail("admin@example.com");
        defaultUser.setPassword("admin-password");
        defaultUser.setCountry(Country.BULGARIA);

        User existingUser = User.builder()
                .email("admin@example.com")
                .role(UserRole.USER)
                .isActive(false)
                .build();

        when(userRepository.findUserByEmail("admin@example.com"))
                .thenReturn(Optional.of(existingUser));

        when(userRepository.save(existingUser))
                .thenReturn(existingUser);

        User result = userService.ensureDefaultAdmin(defaultUser);

        assertSame(existingUser, result);
        assertEquals(UserRole.ADMIN, result.getRole());
        assertTrue(result.getIsActive());
        assertNotNull(result.getUpdatedOn());

        verify(userRepository)
                .findUserByEmail("admin@example.com");
        verify(userRepository).save(existingUser);

        verifyNoInteractions(passwordEncoder);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void ensureDefaultAdminShouldReturnExistingActiveAdminWithoutSaving() {
        UserProperties.DefaultUser defaultUser =
                new UserProperties.DefaultUser();

        defaultUser.setName("Main Admin");
        defaultUser.setEmail("admin@example.com");
        defaultUser.setPassword("admin-password");
        defaultUser.setCountry(Country.BULGARIA);

        User existingAdmin = User.builder()
                .email("admin@example.com")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        when(userRepository.findUserByEmail("admin@example.com"))
                .thenReturn(Optional.of(existingAdmin));

        User result = userService.ensureDefaultAdmin(defaultUser);

        assertSame(existingAdmin, result);
        assertEquals(UserRole.ADMIN, result.getRole());
        assertTrue(result.getIsActive());

        verify(userRepository)
                .findUserByEmail("admin@example.com");

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void getCurrentUserShouldThrowExceptionWhenAuthenticationIsMissing() {
        SecurityContextHolder.clearContext();

        UnauthorizedActionException exception = assertThrows(
                UnauthorizedActionException.class,
                () -> userService.getCurrentUser()
        );

        assertEquals(
                "User must be logged in.",
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void getCurrentUserShouldThrowExceptionWhenUserDoesNotExist() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "missing@example.com",
                        "password",
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        when(userRepository.findUserByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getCurrentUser()
        );

        assertEquals(
                "User not found.",
                exception.getMessage()
        );

        verify(userRepository)
                .findUserByEmail("missing@example.com");
        verifyNoInteractions(passwordEncoder);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void updateCurrentUserProfileShouldUpdateUserAndReturnTrueWhenEmailChanges() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "old@example.com",
                        "password",
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .name("Old Name")
                .email("old@example.com")
                .country(Country.GREECE)
                .gender(Gender.MALE)
                .profilePicture("/images/default-avatar-man.png")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        UpdateProfileRequest request =
                UpdateProfileRequest.builder()
                        .name("  Maria Ivanova  ")
                        .email("  MARIA@EXAMPLE.COM  ")
                        .country(Country.BULGARIA)
                        .gender(Gender.FEMALE)
                        .profilePicture("")
                        .build();

        when(userRepository.findUserByEmail("old@example.com"))
                .thenReturn(Optional.of(existingUser));

        when(userRepository.existsByEmailAndIdNot(
                "maria@example.com",
                existingUser.getId()
        )).thenReturn(false);

        boolean emailChanged =
                userService.updateCurrentUserProfile(request);

        assertTrue(emailChanged);
        assertEquals("Maria Ivanova", existingUser.getName());
        assertEquals(
                "maria@example.com",
                existingUser.getEmail()
        );
        assertEquals(
                Country.BULGARIA,
                existingUser.getCountry()
        );
        assertEquals(Gender.FEMALE, existingUser.getGender());
        assertEquals(
                "/images/default-avatar-woman.png",
                existingUser.getProfilePicture()
        );
        assertNotNull(existingUser.getUpdatedOn());

        verify(userRepository)
                .findUserByEmail("old@example.com");

        verify(userRepository).existsByEmailAndIdNot(
                "maria@example.com",
                existingUser.getId()
        );

        verify(userRepository).save(existingUser);
        verifyNoInteractions(passwordEncoder);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void updateCurrentUserProfileShouldThrowExceptionWhenEmailExists() {
        UUID userId = UUID.randomUUID();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "old@example.com",
                        "password",
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        User existingUser = User.builder()
                .id(userId)
                .name("Old Name")
                .email("old@example.com")
                .country(Country.GREECE)
                .gender(Gender.MALE)
                .role(UserRole.USER)
                .isActive(true)
                .build();

        UpdateProfileRequest request =
                UpdateProfileRequest.builder()
                        .name("New Name")
                        .email("existing@example.com")
                        .country(Country.BULGARIA)
                        .gender(Gender.FEMALE)
                        .build();

        when(userRepository.findUserByEmail("old@example.com"))
                .thenReturn(Optional.of(existingUser));

        when(userRepository.existsByEmailAndIdNot(
                "existing@example.com",
                userId
        )).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.updateCurrentUserProfile(request)
        );

        assertEquals(
                "User with this email already exists.",
                exception.getMessage()
        );

        assertEquals("Old Name", existingUser.getName());
        assertEquals(
                "old@example.com",
                existingUser.getEmail()
        );

        verify(userRepository)
                .findUserByEmail("old@example.com");

        verify(userRepository).existsByEmailAndIdNot(
                "existing@example.com",
                userId
        );

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void activateUserShouldActivateAndSaveUser() {
        UUID userId = UUID.randomUUID();

        User inactiveUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .role(UserRole.USER)
                .isActive(false)
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(inactiveUser));

        when(userRepository.save(inactiveUser))
                .thenReturn(inactiveUser);

        User result = userService.activateUser(userId);

        assertSame(inactiveUser, result);
        assertTrue(result.getIsActive());
        assertNotNull(result.getUpdatedOn());

        verify(userRepository).findById(userId);
        verify(userRepository).save(inactiveUser);
        verifyNoInteractions(passwordEncoder);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void deactivateUserShouldDeactivateAnotherUser() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com",
                        "password",
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        User currentUser = User.builder()
                .id(currentUserId)
                .email("admin@example.com")
                .role(UserRole.USER_ADMIN)
                .isActive(true)
                .build();

        User targetUser = User.builder()
                .id(targetUserId)
                .email("target@example.com")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        when(userRepository.findById(targetUserId))
                .thenReturn(Optional.of(targetUser));

        when(userRepository.findUserByEmail("admin@example.com"))
                .thenReturn(Optional.of(currentUser));

        when(userRepository.save(targetUser))
                .thenReturn(targetUser);

        User result = userService.deactivateUser(targetUserId);

        assertSame(targetUser, result);
        assertFalse(result.getIsActive());
        assertNotNull(result.getUpdatedOn());

        verify(userRepository).findById(targetUserId);
        verify(userRepository)
                .findUserByEmail("admin@example.com");
        verify(userRepository).save(targetUser);

        verifyNoInteractions(passwordEncoder);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void deactivateUserShouldThrowExceptionWhenTargetIsCurrentUser() {
        UUID userId = UUID.randomUUID();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com",
                        "password",
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        User currentUser = User.builder()
                .id(userId)
                .email("admin@example.com")
                .role(UserRole.USER_ADMIN)
                .isActive(true)
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(currentUser));

        when(userRepository.findUserByEmail("admin@example.com"))
                .thenReturn(Optional.of(currentUser));

        UnauthorizedActionException exception = assertThrows(
                UnauthorizedActionException.class,
                () -> userService.deactivateUser(userId)
        );

        assertEquals(
                "You cannot deactivate your own account.",
                exception.getMessage()
        );

        assertTrue(currentUser.getIsActive());

        verify(userRepository).findById(userId);
        verify(userRepository)
                .findUserByEmail("admin@example.com");
        verify(userRepository, never()).save(any(User.class));

        verifyNoInteractions(passwordEncoder);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void deactivateUserShouldThrowExceptionWhenTargetIsMainAdmin() {
        UUID currentUserId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "user-admin@example.com",
                        "password",
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        User currentUser = User.builder()
                .id(currentUserId)
                .email("user-admin@example.com")
                .role(UserRole.USER_ADMIN)
                .isActive(true)
                .build();

        User mainAdmin = User.builder()
                .id(adminId)
                .email("main-admin@example.com")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        when(userRepository.findById(adminId))
                .thenReturn(Optional.of(mainAdmin));

        when(userRepository.findUserByEmail(
                "user-admin@example.com"
        )).thenReturn(Optional.of(currentUser));

        UnauthorizedActionException exception = assertThrows(
                UnauthorizedActionException.class,
                () -> userService.deactivateUser(adminId)
        );

        assertEquals(
                "The main administrator cannot be deactivated.",
                exception.getMessage()
        );

        assertTrue(mainAdmin.getIsActive());

        verify(userRepository).findById(adminId);
        verify(userRepository)
                .findUserByEmail("user-admin@example.com");
        verify(userRepository, never()).save(any(User.class));

        verifyNoInteractions(passwordEncoder);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void updateCurrentUserProfileShouldReplaceDefaultPictureWithCustomUrl() {
        String customPictureUrl =
                "https://cdn.example.com/avatar.png";

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "user@example.com",
                        "password",
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .name("Old Name")
                .email("user@example.com")
                .country(Country.BULGARIA)
                .gender(Gender.MALE)
                .profilePicture(
                        "/images/default-avatar-man.png"
                )
                .role(UserRole.USER)
                .isActive(true)
                .build();

        UpdateProfileRequest request =
                UpdateProfileRequest.builder()
                        .name("Updated Name")
                        .email("user@example.com")
                        .country(Country.BULGARIA)
                        .gender(Gender.MALE)
                        .profilePicture(
                                "  " + customPictureUrl + "  "
                        )
                        .build();

        when(userRepository.findUserByEmail(
                "user@example.com"
        )).thenReturn(Optional.of(existingUser));

        boolean emailChanged =
                userService.updateCurrentUserProfile(request);

        assertFalse(emailChanged);

        assertEquals(
                customPictureUrl,
                existingUser.getProfilePicture()
        );

        assertNotNull(existingUser.getUpdatedOn());

        verify(userRepository)
                .findUserByEmail("user@example.com");

        verify(userRepository).save(existingUser);

        verifyNoInteractions(passwordEncoder);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void findByIdShouldRejectNullId() {
        InvalidUserDataException exception =
                assertThrows(
                        InvalidUserDataException.class,
                        () -> userService.findById(null)
                );

        assertEquals(
                "User id is required.",
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void registerShouldRejectNullRequest() {
        InvalidUserDataException exception =
                assertThrows(
                        InvalidUserDataException.class,
                        () -> userService.register(null)
                );

        assertEquals(
                "Registration request is required.",
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrationRequests")
    void registerShouldRejectInvalidData(
            String name,
            String email,
            String password,
            Country country,
            Gender gender,
            String expectedMessage) {

        RegisterRequest request =
                RegisterRequest.builder()
                        .name(name)
                        .email(email)
                        .password(password)
                        .country(country)
                        .gender(gender)
                        .build();

        InvalidUserDataException exception =
                assertThrows(
                        InvalidUserDataException.class,
                        () -> userService.register(request)
                );

        assertEquals(
                expectedMessage,
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }

    private static Stream<Arguments>
    invalidRegistrationRequests() {

        return Stream.of(
                Arguments.of(
                        (String) null,
                        "ivan@example.com",
                        "password123",
                        Country.BULGARIA,
                        Gender.MALE,
                        "User name is required."
                ),
                Arguments.of(
                        "  ",
                        "ivan@example.com",
                        "password123",
                        Country.BULGARIA,
                        Gender.MALE,
                        "User name is required."
                ),
                Arguments.of(
                        "ab",
                        "ivan@example.com",
                        "password123",
                        Country.BULGARIA,
                        Gender.MALE,
                        "User name must be between 3 and 50 symbols."
                ),
                Arguments.of(
                        "a".repeat(51),
                        "ivan@example.com",
                        "password123",
                        Country.BULGARIA,
                        Gender.MALE,
                        "User name must be between 3 and 50 symbols."
                ),
                Arguments.of(
                        "Ivan Ivanov",
                        (String) null,
                        "password123",
                        Country.BULGARIA,
                        Gender.MALE,
                        "Email is required."
                ),
                Arguments.of(
                        "Ivan Ivanov",
                        "invalid-email",
                        "password123",
                        Country.BULGARIA,
                        Gender.MALE,
                        "Please enter a valid email address."
                ),
                Arguments.of(
                        "Ivan Ivanov",
                        "a".repeat(90) + "@example.com",
                        "password123",
                        Country.BULGARIA,
                        Gender.MALE,
                        "Email must be up to 100 symbols."
                ),
                Arguments.of(
                        "Ivan Ivanov",
                        "ivan@example.com",
                        (String) null,
                        Country.BULGARIA,
                        Gender.MALE,
                        "Password is required."
                ),
                Arguments.of(
                        "Ivan Ivanov",
                        "ivan@example.com",
                        "short",
                        Country.BULGARIA,
                        Gender.MALE,
                        "Password must be between 6 and 100 symbols."
                ),
                Arguments.of(
                        "Ivan Ivanov",
                        "ivan@example.com",
                        "a".repeat(101),
                        Country.BULGARIA,
                        Gender.MALE,
                        "Password must be between 6 and 100 symbols."
                ),
                Arguments.of(
                        "Ivan Ivanov",
                        "ivan@example.com",
                        "password123",
                        (Country) null,
                        Gender.MALE,
                        "Country is required."
                ),
                Arguments.of(
                        "Ivan Ivanov",
                        "ivan@example.com",
                        "password123",
                        Country.BULGARIA,
                        (Gender) null,
                        "Gender is required."
                )
        );
    }

    @Test
    void updateCurrentUserProfileShouldRejectNullRequest() {
        InvalidUserDataException exception =
                assertThrows(
                        InvalidUserDataException.class,
                        () -> userService
                                .updateCurrentUserProfile(null)
                );

        assertEquals(
                "Profile update request is required.",
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @ParameterizedTest
    @MethodSource("invalidProfileUpdateRequests")
    void updateCurrentUserProfileShouldRejectInvalidData(
            String name,
            String email,
            Country country,
            Gender gender,
            String profilePicture,
            String expectedMessage) {

        UpdateProfileRequest request =
                UpdateProfileRequest.builder()
                        .name(name)
                        .email(email)
                        .country(country)
                        .gender(gender)
                        .profilePicture(profilePicture)
                        .build();

        InvalidUserDataException exception =
                assertThrows(
                        InvalidUserDataException.class,
                        () -> userService
                                .updateCurrentUserProfile(request)
                );

        assertEquals(
                expectedMessage,
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }

    private static Stream<Arguments>
    invalidProfileUpdateRequests() {

        return Stream.of(
                Arguments.of(
                        (String) null,
                        "user@example.com",
                        Country.BULGARIA,
                        Gender.MALE,
                        "",
                        "User name is required."
                ),
                Arguments.of(
                        "Ivan Ivanov",
                        "invalid-email",
                        Country.BULGARIA,
                        Gender.MALE,
                        "",
                        "Please enter a valid email address."
                ),
                Arguments.of(
                        "Ivan Ivanov",
                        "user@example.com",
                        (Country) null,
                        Gender.MALE,
                        "",
                        "Country is required."
                ),
                Arguments.of(
                        "Ivan Ivanov",
                        "user@example.com",
                        Country.BULGARIA,
                        (Gender) null,
                        "",
                        "Gender is required."
                ),
                Arguments.of(
                        "Ivan Ivanov",
                        "user@example.com",
                        Country.BULGARIA,
                        Gender.MALE,
                        "a".repeat(501),
                        "Profile picture URL must be up to 500 symbols."
                )
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRoleUpdateArguments")
    void updateUserRoleShouldRejectInvalidArguments(
            UUID userId,
            UserRole newRole,
            String expectedMessage) {

        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.updateUserRole(userId, newRole)
        );

        assertEquals(expectedMessage, exception.getMessage());

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }

    private static Stream<Arguments> invalidRoleUpdateArguments() {
        return Stream.of(
                Arguments.of(
                        (UUID) null,
                        UserRole.USER,
                        "User id is required."
                ),
                Arguments.of(
                        UUID.randomUUID(),
                        (UserRole) null,
                        "Ролята е задължителна."
                )
        );
    }
}
