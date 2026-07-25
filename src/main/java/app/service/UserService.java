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

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest registerRequest){

        String email = registerRequest.getEmail().trim().toLowerCase();
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
                .name(registerRequest.getName())
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

        String email = defaultUser.getEmail().trim().toLowerCase();
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
        Country country = defaultUser.getCountry();

        User admin = User.builder()
                .name(defaultUser.getName())
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

    public User findById (UUID id){
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found."));
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
        User currentUser = getCurrentUser();
        User targetUser = findById(userId);

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException(
                    "Не можете да промените собствената си роля."
            );
        }

        if (newRole == null) {
            throw new InvalidUserDataException(
                    "Ролята е задължителна."
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
        User user = getCurrentUser();

        String normalizedEmail = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

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

        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        user.setCountry(request.getCountry());
        user.setGender(request.getGender());

        String profilePicture = request.getProfilePicture();

        String currentProfilePicture = user.getProfilePicture();

        boolean usesDefaultPicture =
                profilePicture == null
                        || profilePicture.isBlank()
                        || currentProfilePicture == null
                        || currentProfilePicture.equals("/images/default-avatar-man.png")
                        || currentProfilePicture.equals("/images/default-avatar-woman.png");

        if (usesDefaultPicture) {
            profilePicture = request.getGender() == Gender.MALE
                    ? "/images/default-avatar-man.png"
                    : "/images/default-avatar-woman.png";
        } else {
            profilePicture = profilePicture.trim();
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
}
