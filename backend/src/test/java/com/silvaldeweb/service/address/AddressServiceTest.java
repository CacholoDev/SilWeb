package com.silvaldeweb.service.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.silvaldeweb.dto.address.AddressCreateRequest;
import com.silvaldeweb.dto.address.AddressResponse;
import com.silvaldeweb.dto.address.AddressUpdateRequest;
import com.silvaldeweb.exception.address.AddressNotFoundException;
import com.silvaldeweb.exception.user.UserNotFoundException;
import com.silvaldeweb.model.address.Address;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.address.AddressRepository;
import com.silvaldeweb.repository.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressService addressService;

    private User sampleUser(Long id) {
        return User.builder().id(id).email("user" + id + "@example.com").role(com.silvaldeweb.model.user.Role.USER).active(true).build();
    }

    private Address sampleAddress(Long id, User user) {
        return Address.builder()
                .id(id)
                .fullName("John Doe")
                .street("Calle Mayor 1")
                .city("Madrid")
                .province("Madrid")
                .postalCode("28001")
                .country("España")
                .isDefault(true)
                .user(user)
                .build();
    }

    @Test
    void createSavesAddressWithJwtActor() {
        User actor = sampleUser(2L);
        AddressCreateRequest request = new AddressCreateRequest(
                "John Doe", "Calle Mayor 1", "Madrid", "Madrid", "28001", "España", true
        );

        when(userRepository.findById(2L)).thenReturn(Optional.of(actor));
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> {
            Address a = i.getArgument(0);
            a.setId(10L);
            return a;
        });

        AddressResponse response = addressService.create(actor, request);

        assertEquals(10L, response.id());
        assertEquals("John Doe", response.fullName());
        assertEquals("28001", response.postalCode());
        assertEquals(true, response.isDefault());
        assertEquals(2L, response.userId());
    }

    @Test
    void createDefaultsIsDefaultToFalseWhenNull() {
        User actor = sampleUser(2L);
        AddressCreateRequest request = new AddressCreateRequest(
                "Jane", "Calle 2", "Madrid", "Madrid", "28002", "España", null
        );

        when(userRepository.findById(2L)).thenReturn(Optional.of(actor));
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> i.getArgument(0));

        AddressResponse response = addressService.create(actor, request);

        assertEquals(false, response.isDefault());
    }

    @Test
    void createThrowsUserNotFoundWhenJwtActorMissing() {
        User actor = sampleUser(99L);
        AddressCreateRequest request = new AddressCreateRequest(
                "John", "Calle 1", "Madrid", "Madrid", "28001", "España", true
        );
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> addressService.create(actor, request));
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void getThrowsWhenNotFound() {
        User actor = sampleUser(2L);
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AddressNotFoundException.class, () -> addressService.get(actor, false, 99L));
    }

    @Test
    void getRejectsAccessToAddressOfAnotherUser() {
        User owner = sampleUser(2L);
        User other = sampleUser(3L);
        Address existing = sampleAddress(10L, owner);
        when(addressRepository.findById(10L)).thenReturn(Optional.of(existing));

        assertThrows(AccessDeniedException.class, () -> addressService.get(other, false, 10L));
    }

    @Test
    void getAllowsAdminToAccessAnyAddress() {
        User owner = sampleUser(2L);
        User admin = sampleUser(1L);
        Address existing = sampleAddress(10L, owner);
        when(addressRepository.findById(10L)).thenReturn(Optional.of(existing));

        AddressResponse response = addressService.get(admin, true, 10L);

        assertEquals(10L, response.id());
        assertEquals(2L, response.userId());
    }

    @Test
    void updateChangesFields() {
        User actor = sampleUser(2L);
        Address existing = sampleAddress(10L, actor);
        when(addressRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> i.getArgument(0));

        AddressUpdateRequest request = new AddressUpdateRequest(
                "Jane Doe", "Calle Nueva 5", "Barcelona", "Barcelona", "08001", "España", false
        );

        AddressResponse response = addressService.update(actor, false, 10L, request);

        assertEquals("Jane Doe", response.fullName());
        assertEquals("Calle Nueva 5", response.street());
        assertEquals("Barcelona", response.city());
        assertEquals("08001", response.postalCode());
        assertEquals(false, response.isDefault());
    }

    @Test
    void updateRejectsAccessToAddressOfAnotherUser() {
        User owner = sampleUser(2L);
        User other = sampleUser(3L);
        Address existing = sampleAddress(10L, owner);
        when(addressRepository.findById(10L)).thenReturn(Optional.of(existing));

        AddressUpdateRequest request = new AddressUpdateRequest(
                "Hacker", "X", "X", "X", "00000", "X", false
        );
        assertThrows(AccessDeniedException.class, () -> addressService.update(other, false, 10L, request));
    }

    @Test
    void deleteRejectsAccessToAddressOfAnotherUser() {
        User owner = sampleUser(2L);
        User other = sampleUser(3L);
        Address existing = sampleAddress(10L, owner);
        when(addressRepository.findById(10L)).thenReturn(Optional.of(existing));

        assertThrows(AccessDeniedException.class, () -> addressService.delete(other, false, 10L));
    }

    @Test
    void deleteThrowsWhenNotFound() {
        User actor = sampleUser(2L);
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AddressNotFoundException.class, () -> addressService.delete(actor, false, 99L));
    }
}
