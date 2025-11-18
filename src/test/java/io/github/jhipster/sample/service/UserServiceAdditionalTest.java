package io.github.jhipster.sample.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jhipster.sample.IntegrationTest;
import io.github.jhipster.sample.domain.Authority;
import io.github.jhipster.sample.domain.User;
import io.github.jhipster.sample.repository.AuthorityRepository;
import io.github.jhipster.sample.repository.UserRepository;
import io.github.jhipster.sample.security.AuthoritiesConstants;
import io.github.jhipster.sample.service.dto.AdminUserDTO;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.security.RandomUtil;

/**
 * Additional integration tests for {@link UserService} to improve coverage.
 */
@IntegrationTest
@Transactional
class UserServiceAdditionalTest {

    private static final String DEFAULT_LOGIN = "testuser_additional";
    private static final String DEFAULT_EMAIL = "testuser_additional@localhost";
    private static final String DEFAULT_FIRSTNAME = "Test";
    private static final String DEFAULT_LASTNAME = "User";
    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";
    private static final String DEFAULT_LANGKEY = "en";

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private Long numberOfUsers;

    @BeforeEach
    void countUsers() {
        numberOfUsers = userRepository.count();
    }

    @BeforeEach
    void init() {
        user = new User();
        user.setLogin(DEFAULT_LOGIN);
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setActivated(true);
        user.setEmail(DEFAULT_EMAIL);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);
    }

    @AfterEach
    void cleanupAndCheck() {
        cacheManager
            .getCacheNames()
            .stream()
            .map(cacheName -> this.cacheManager.getCache(cacheName))
            .filter(Objects::nonNull)
            .forEach(Cache::clear);
        userService.deleteUser(DEFAULT_LOGIN);
        assertThat(userRepository.count()).isEqualTo(numberOfUsers);
        numberOfUsers = null;
    }

    @Test
    @Transactional
    void assertThatActivateRegistrationWorks() {
        user.setActivated(false);
        user.setActivationKey("activation-key-123");
        userRepository.saveAndFlush(user);

        Optional<User> activatedUser = userService.activateRegistration("activation-key-123");
        assertThat(activatedUser).isPresent();
        assertThat(activatedUser.orElseThrow().isActivated()).isTrue();
        assertThat(activatedUser.orElseThrow().getActivationKey()).isNull();
    }

    @Test
    @Transactional
    void assertThatActivateRegistrationWithInvalidKeyReturnsEmpty() {
        user.setActivated(false);
        user.setActivationKey("activation-key-123");
        userRepository.saveAndFlush(user);

        Optional<User> activatedUser = userService.activateRegistration("invalid-key");
        assertThat(activatedUser).isEmpty();
    }

    @Test
    @Transactional
    void assertThatRegisterUserCreatesUserWithCorrectProperties() {
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("newuser");
        userDTO.setEmail("newuser@example.com");
        userDTO.setFirstName("New");
        userDTO.setLastName("User");
        userDTO.setLangKey("en");
        userDTO.setImageUrl("http://placehold.it/50x50");

        User registeredUser = userService.registerUser(userDTO, "password123");
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getLogin()).isEqualTo("newuser");
        assertThat(registeredUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(registeredUser.isActivated()).isFalse();
        assertThat(registeredUser.getActivationKey()).isNotNull();
        assertThat(registeredUser.getAuthorities()).hasSize(1);
        assertThat(registeredUser.getAuthorities().iterator().next().getName()).isEqualTo(AuthoritiesConstants.USER);

        userService.deleteUser("newuser");
    }

    @Test
    @Transactional
    void assertThatRegisterUserWithExistingLoginThrowsException() {
        userRepository.saveAndFlush(user);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin(DEFAULT_LOGIN);
        userDTO.setEmail("different@example.com");
        userDTO.setFirstName("New");
        userDTO.setLastName("User");

        assertThatThrownBy(() -> userService.registerUser(userDTO, "password123")).isInstanceOf(UsernameAlreadyUsedException.class);
    }

    @Test
    @Transactional
    void assertThatRegisterUserWithExistingEmailThrowsException() {
        userRepository.saveAndFlush(user);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("different");
        userDTO.setEmail(DEFAULT_EMAIL);
        userDTO.setFirstName("New");
        userDTO.setLastName("User");

        assertThatThrownBy(() -> userService.registerUser(userDTO, "password123")).isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    @Transactional
    void assertThatRegisterUserRemovesNonActivatedUserWithSameLogin() {
        User nonActivatedUser = new User();
        nonActivatedUser.setLogin("duplicate");
        nonActivatedUser.setEmail("duplicate@example.com");
        nonActivatedUser.setPassword(passwordEncoder.encode("oldPassword"));
        nonActivatedUser.setActivated(false);
        nonActivatedUser.setActivationKey("old-key");
        userRepository.saveAndFlush(nonActivatedUser);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("duplicate");
        userDTO.setEmail("duplicate@example.com");
        userDTO.setFirstName("New");
        userDTO.setLastName("User");

        User registeredUser = userService.registerUser(userDTO, "password123");
        assertThat(registeredUser).isNotNull();
        assertThat(userRepository.findOneByLogin("duplicate")).isPresent();

        userService.deleteUser("duplicate");
    }

    @Test
    @Transactional
    void assertThatCreateUserWorks() {
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("adminuser");
        userDTO.setEmail("adminuser@example.com");
        userDTO.setFirstName("Admin");
        userDTO.setLastName("User");
        userDTO.setActivated(true);
        Set<String> authorities = new HashSet<>();
        authorities.add(AuthoritiesConstants.ADMIN);
        userDTO.setAuthorities(authorities);

        User createdUser = userService.createUser(userDTO);
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getLogin()).isEqualTo("adminuser");
        assertThat(createdUser.isActivated()).isTrue();
        assertThat(createdUser.getAuthorities()).hasSize(1);
        assertThat(createdUser.getAuthorities().iterator().next().getName()).isEqualTo(AuthoritiesConstants.ADMIN);

        userService.deleteUser("adminuser");
    }

    @Test
    @Transactional
    void assertThatCreateUserWithDefaultLanguageWorks() {
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("defaultlang");
        userDTO.setEmail("defaultlang@example.com");
        userDTO.setFirstName("Default");
        userDTO.setLastName("Lang");
        userDTO.setLangKey(null); // Should use default

        User createdUser = userService.createUser(userDTO);
        assertThat(createdUser.getLangKey()).isNotNull();

        userService.deleteUser("defaultlang");
    }

    @Test
    @Transactional
    void assertThatUpdateUserWorks() {
        userRepository.saveAndFlush(user);

        AdminUserDTO userDTO = new AdminUserDTO(user);
        userDTO.setFirstName("Updated");
        userDTO.setLastName("Name");
        userDTO.setEmail("updated@example.com");

        Optional<AdminUserDTO> updatedUser = userService.updateUser(userDTO);
        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.orElseThrow().getFirstName()).isEqualTo("Updated");
        assertThat(updatedUser.orElseThrow().getLastName()).isEqualTo("Name");
        assertThat(updatedUser.orElseThrow().getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    @Transactional
    void assertThatUpdateUserWithNonExistentIdReturnsEmpty() {
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setId(Long.MAX_VALUE);
        userDTO.setLogin("nonexistent");
        userDTO.setEmail("nonexistent@example.com");

        Optional<AdminUserDTO> updatedUser = userService.updateUser(userDTO);
        assertThat(updatedUser).isEmpty();
    }

    @Test
    @Transactional
    void assertThatUpdateUserWithAuthoritiesWorks() {
        userRepository.saveAndFlush(user);

        Authority adminAuthority = authorityRepository.findById(AuthoritiesConstants.ADMIN).orElseThrow();
        Set<String> authorities = new HashSet<>();
        authorities.add(AuthoritiesConstants.ADMIN);

        AdminUserDTO userDTO = new AdminUserDTO(user);
        userDTO.setAuthorities(authorities);

        Optional<AdminUserDTO> updatedUser = userService.updateUser(userDTO);
        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.orElseThrow().getAuthorities()).contains(AuthoritiesConstants.ADMIN);
    }

    @Test
    @Transactional
    void assertThatDeleteUserWorks() {
        userRepository.saveAndFlush(user);

        userService.deleteUser(DEFAULT_LOGIN);

        assertThat(userRepository.findOneByLogin(DEFAULT_LOGIN)).isEmpty();
    }

    @Test
    @Transactional
    void assertThatDeleteNonExistentUserDoesNotThrow() {
        // Should not throw exception
        userService.deleteUser("nonexistent");
    }

    @Test
    @Transactional
    @WithMockUser(username = "testuser_additional")
    void assertThatUpdateUserBasicInfoWorks() {
        userRepository.saveAndFlush(user);

        userService.updateUser("UpdatedFirst", "UpdatedLast", "updated@example.com", "fr", "http://newimage.com");

        User updatedUser = userRepository.findOneByLogin(DEFAULT_LOGIN).orElseThrow();
        assertThat(updatedUser.getFirstName()).isEqualTo("UpdatedFirst");
        assertThat(updatedUser.getLastName()).isEqualTo("UpdatedLast");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getLangKey()).isEqualTo("fr");
        assertThat(updatedUser.getImageUrl()).isEqualTo("http://newimage.com");
    }

    @Test
    @Transactional
    @WithMockUser(username = "testuser_additional")
    void assertThatUpdateUserBasicInfoWithNullEmailWorks() {
        userRepository.saveAndFlush(user);

        userService.updateUser("UpdatedFirst", "UpdatedLast", null, "fr", null);

        User updatedUser = userRepository.findOneByLogin(DEFAULT_LOGIN).orElseThrow();
        assertThat(updatedUser.getFirstName()).isEqualTo("UpdatedFirst");
        assertThat(updatedUser.getEmail()).isEqualTo(DEFAULT_EMAIL); // Should remain unchanged
    }

    @Test
    @Transactional
    @WithMockUser(username = "testuser_additional")
    void assertThatChangePasswordWithWrongCurrentPasswordThrowsException() {
        String currentPassword = "currentPassword";
        user.setPassword(passwordEncoder.encode(currentPassword));
        userRepository.saveAndFlush(user);

        assertThatThrownBy(() -> userService.changePassword("wrongPassword", "newPassword")).isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    @Transactional
    @WithMockUser(username = "testuser_additional")
    void assertThatChangePasswordWorks() {
        String currentPassword = "currentPassword";
        user.setPassword(passwordEncoder.encode(currentPassword));
        userRepository.saveAndFlush(user);

        userService.changePassword(currentPassword, "newPassword");

        User updatedUser = userRepository.findOneByLogin(DEFAULT_LOGIN).orElseThrow();
        assertThat(passwordEncoder.matches("newPassword", updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional
    void assertThatGetAllManagedUsersWorks() {
        userRepository.saveAndFlush(user);

        Pageable pageable = PageRequest.of(0, 10);
        Page<AdminUserDTO> users = userService.getAllManagedUsers(pageable);

        assertThat(users).isNotNull();
        assertThat(users.getContent()).isNotEmpty();
    }

    @Test
    @Transactional
    void assertThatGetAllPublicUsersWorks() {
        user.setActivated(true);
        userRepository.saveAndFlush(user);

        Pageable pageable = PageRequest.of(0, 10);
        Page<io.github.jhipster.sample.service.dto.UserDTO> users = userService.getAllPublicUsers(pageable);

        assertThat(users).isNotNull();
        assertThat(users.getContent()).isNotEmpty();
    }

    @Test
    @Transactional
    void assertThatGetAllPublicUsersExcludesNonActivated() {
        user.setActivated(false);
        userRepository.saveAndFlush(user);

        Pageable pageable = PageRequest.of(0, 10);
        Page<io.github.jhipster.sample.service.dto.UserDTO> users = userService.getAllPublicUsers(pageable);

        assertThat(users.getContent().stream().anyMatch(u -> u.getLogin().equals(DEFAULT_LOGIN))).isFalse();
    }

    @Test
    @Transactional
    void assertThatGetUserWithAuthoritiesByLoginWorks() {
        userRepository.saveAndFlush(user);

        Optional<User> foundUser = userService.getUserWithAuthoritiesByLogin(DEFAULT_LOGIN);
        assertThat(foundUser).isPresent();
        assertThat(foundUser.orElseThrow().getLogin()).isEqualTo(DEFAULT_LOGIN);
    }

    @Test
    @Transactional
    void assertThatGetUserWithAuthoritiesByLoginReturnsEmptyForNonExistent() {
        Optional<User> foundUser = userService.getUserWithAuthoritiesByLogin("nonexistent");
        assertThat(foundUser).isEmpty();
    }

    @Test
    @Transactional
    void assertThatGetAuthoritiesWorks() {
        Set<String> authorities = new HashSet<>(userService.getAuthorities());
        assertThat(authorities).isNotEmpty();
        assertThat(authorities).contains(AuthoritiesConstants.USER);
    }

    @Test
    @Transactional
    void assertThatCompletePasswordResetWithExpiredKeyReturnsEmpty() {
        Instant expiredDate = Instant.now().minus(2, ChronoUnit.DAYS);
        user.setActivated(true);
        user.setResetDate(expiredDate);
        user.setResetKey("reset-key");
        userRepository.saveAndFlush(user);

        Optional<User> result = userService.completePasswordReset("newPassword", "reset-key");
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void assertThatCompletePasswordResetWithValidKeyWorks() {
        Instant validDate = Instant.now().minus(1, ChronoUnit.HOURS);
        String oldPassword = user.getPassword();
        user.setActivated(true);
        user.setResetDate(validDate);
        user.setResetKey("reset-key");
        userRepository.saveAndFlush(user);

        Optional<User> result = userService.completePasswordReset("newPassword", "reset-key");
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getResetKey()).isNull();
        assertThat(result.orElseThrow().getResetDate()).isNull();
        assertThat(passwordEncoder.matches("newPassword", result.orElseThrow().getPassword())).isTrue();
    }
}
