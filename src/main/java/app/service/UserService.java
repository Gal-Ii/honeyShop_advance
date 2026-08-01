package app.service;

import app.exception.*;
import app.model.entity.user.Gender;
import app.model.entity.user.Country;
import app.web.dto.user.UpdateProfileRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import app.model.entity.user.User;
import app.model.entity.user.UserProperties;
import app.model.entity.user.UserRole;
import app.repository.user.UserRepository;
import app.web.dto.user.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
            );

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest registerRequest){

        if (registerRequest == null) {
            throw new InvalidUserDataException(
                    "Registration request is required."
            );
        }

        String name = normalizeAndValidateName(
                registerRequest.getName()
        );

        String email = normalizeAndValidateEmail(
                registerRequest.getEmail()
        );

        validatePassword(registerRequest.getPassword());

        validateCountryAndGender(
                registerRequest.getCountry(),
                registerRequest.getGender()
        );

        Optional<User>optionalUser = userRepository.findUserByEmail(email);

        if (optionalUser.isPresent()){
            throw new UserAlreadyExistsException("User with [%s] email already exists.".formatted(registerRequest.getEmail()));
        }

        String profilePicture;

        if (registerRequest.getGender() == Gender.MALE) {
            profilePicture = "/images/default-avatar-man.png";
        } else {
            profilePicture = "/images/default-avatar-woman.png";
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .country(registerRequest.getCountry())
                .gender(registerRequest.getGender())
                .profilePicture(profilePicture)
                .role(UserRole.USER)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        log.info(
                "User registered: userId={}, role={}",
                savedUser.getId(),
                savedUser.getRole()
        );

        return savedUser;

    }

    public User ensureDefaultAdmin(UserProperties.DefaultUser defaultUser) {
        if (defaultUser == null) {
            throw new InvalidUserDataException("Default user properties are required.");
        }

        String name = normalizeAndValidateName(
                defaultUser.getName()
        );

        String email = normalizeAndValidateEmail(
                defaultUser.getEmail()
        );

        validatePassword(defaultUser.getPassword());

        Country country = defaultUser.getCountry();
        validateCountry(country);

        Optional<User> optionalUser = userRepository.findUserByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getRole() != UserRole.ADMIN || !Boolean.TRUE.equals(user.getIsActive())) {
                user.setRole(UserRole.ADMIN);
                user.setIsActive(true);
                user.setUpdatedOn(LocalDateTime.now());
                User savedAdmin = userRepository.save(user);

                log.info(
                        "Default admin restored: userId={}",
                        savedAdmin.getId()
                );

                return savedAdmin;
            }

            return user;
        }

        LocalDateTime now = LocalDateTime.now();

        User admin = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(defaultUser.getPassword()))
                .country(country)
                .gender(Gender.FEMALE)
                .profilePicture("/images/default-avatar-woman.png")
                .role(UserRole.ADMIN)
                .isActive(true)
                .createdOn(now)
                .updatedOn(now)
                .build();

        User savedAdmin = userRepository.save(admin);

        log.info(
                "Default admin created: userId={}",
                savedAdmin.getId()
        );

        return savedAdmin;
    }

    public User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedActionException(
                    "User must be logged in."
            );
        }

        return userRepository.findUserByEmail(authentication.getName())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found."));
    }

    public User findById(UUID id) {

        if (id == null) {
            throw new InvalidUserDataException(
                    "User id is required."
            );
        }

        return userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found."
                        ));
    }

    public List<User> getAll () {
            return userRepository.findAll();
    }


    public boolean hasAdminPermission() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("PRODUCT_CREATE")
                        || authority.equals("PRODUCT_UPDATE")
                        || authority.equals("PRODUCT_DELETE")
                        || authority.equals("ORDER_STATUS_UPDATE")
                        || authority.equals("USER_VIEW")
                        || authority.equals("USER_ACTIVATE")
                        || authority.equals("USER_DEACTIVATE")
                        || authority.equals("USER_ROLE_UPDATE"));
    }

    public boolean isLoggedIn () {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    public boolean hasAnyAuthority(String... requiredAuthorities) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }

        Set<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return Arrays.stream(requiredAuthorities)
                .anyMatch(authorities::contains);
    }

    @PreAuthorize("hasAuthority('USER_ACTIVATE')")
    public User activateUser(UUID id) {
        User user = findById(id);

        user.setIsActive(true);
        user.setUpdatedOn(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        log.info(
                "User activated: userId={}",
                savedUser.getId()
        );

        return savedUser;
    }

    @PreAuthorize("hasAuthority('USER_DEACTIVATE')")
    public User deactivateUser(UUID id) {
        User targetUser = findById(id);
        User currentUser = getCurrentUser();

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException(
                    "You cannot deactivate your own account."
            );
        }

        if (targetUser.getRole() == UserRole.ADMIN) {
            throw new UnauthorizedActionException(
                    "The main administrator cannot be deactivated."
            );
        }

        targetUser.setIsActive(false);
        targetUser.setUpdatedOn(LocalDateTime.now());

        User savedUser = userRepository.save(targetUser);

        log.info(
                "User deactivated: userId={}, performedBy={}",
                savedUser.getId(),
                currentUser.getId()
        );

        return savedUser;
    }

    @PreAuthorize("hasAuthority('USER_ROLE_UPDATE')")
    public User updateUserRole(UUID userId, UserRole newRole) {

        if (userId == null) {
            throw new InvalidUserDataException(
                    "User id is required."
            );
        }

        if (newRole == null) {
            throw new InvalidUserDataException(
                    "Ролята е задължителна."
            );
        }

        User currentUser = getCurrentUser();
        User targetUser = findById(userId);

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException(
                    "Не можете да промените собствената си роля."
            );
        }

        if (newRole == UserRole.ADMIN) {
            throw new UnauthorizedActionException(
                    "Ролята ADMIN не може да бъде задавана през административния панел."
            );
        }
        UserRole oldRole = targetUser.getRole();
        targetUser.setRole(newRole);
        targetUser.setUpdatedOn(LocalDateTime.now());

        User savedUser = userRepository.save(targetUser);

        log.info(
                "User role updated: userId={}, oldRole={}, newRole={}, performedBy={}",
                savedUser.getId(),
                oldRole,
                savedUser.getRole(),
                currentUser.getId()
        );

        return savedUser;
    }

    @Transactional
    public boolean updateCurrentUserProfile(UpdateProfileRequest request) {

        if (request == null) {
            throw new InvalidUserDataException(
                    "Profile update request is required."
            );
        }

        String normalizedName =
                normalizeAndValidateName(request.getName());

        String normalizedEmail =
                normalizeAndValidateEmail(request.getEmail());

        validateCountryAndGender(
                request.getCountry(),
                request.getGender()
        );

        String normalizedProfilePicture =
                normalizeAndValidateProfilePicture(
                        request.getProfilePicture()
                );

        User user = getCurrentUser();

        boolean emailChanged =
                !user.getEmail().equalsIgnoreCase(normalizedEmail);

        if (emailChanged &&
                userRepository.existsByEmailAndIdNot(
                        normalizedEmail,
                        user.getId())) {

            throw new UserAlreadyExistsException(
                    "User with this email already exists."
            );
        }

        user.setName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setCountry(request.getCountry());
        user.setGender(request.getGender());

        boolean usesDefaultPicture =
                normalizedProfilePicture.isBlank()
                        || normalizedProfilePicture.equals(
                        "/images/default-avatar-man.png"
                )
                        || normalizedProfilePicture.equals(
                        "/images/default-avatar-woman.png"
                );

        String profilePicture;

        if (usesDefaultPicture) {
            profilePicture =
                    request.getGender() == Gender.MALE
                            ? "/images/default-avatar-man.png"
                            : "/images/default-avatar-woman.png";
        } else {
            profilePicture = normalizedProfilePicture;
        }

        user.setProfilePicture(profilePicture);

        user.setUpdatedOn(LocalDateTime.now());

        userRepository.save(user);

        log.info(
                "User profile updated: userId={}, emailChanged={}",
                user.getId(),
                emailChanged
        );

        return emailChanged;
    }

    private String normalizeAndValidateName(String name) {

        if (name == null || name.isBlank()) {
            throw new InvalidUserDataException(
                    "User name is required."
            );
        }

        String normalizedName = name.trim();

        if (normalizedName.length() < 3
                || normalizedName.length() > 50) {

            throw new InvalidUserDataException(
                    "User name must be between 3 and 50 symbols."
            );
        }

        return normalizedName;
    }

    private String normalizeAndValidateEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new InvalidUserDataException(
                    "Email is required."
            );
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        if (normalizedEmail.length() > 100) {
            throw new InvalidUserDataException(
                    "Email must be up to 100 symbols."
            );
        }

        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new InvalidUserDataException(
                    "Please enter a valid email address."
            );
        }

        return normalizedEmail;
    }

    private void validatePassword(String password) {

        if (password == null || password.isBlank()) {
            throw new InvalidUserDataException(
                    "Password is required."
            );
        }

        if (password.length() < 6
                || password.length() > 100) {

            throw new InvalidUserDataException(
                    "Password must be between 6 and 100 symbols."
            );
        }
    }

    private void validateCountryAndGender(
            Country country,
            Gender gender) {

        validateCountry(country);

        if (gender == null) {
            throw new InvalidUserDataException(
                    "Gender is required."
            );
        }
    }

    private String normalizeAndValidateProfilePicture(
            String profilePicture) {

        if (profilePicture == null) {
            return "";
        }

        if (profilePicture.length() > 500) {
            throw new InvalidUserDataException(
                    "Profile picture URL must be up to 500 symbols."
            );
        }

        return profilePicture.trim();
    }

    private void validateCountry(Country country) {

        if (country == null) {
            throw new InvalidUserDataException(
                    "Country is required."
            );
        }
    }
}
