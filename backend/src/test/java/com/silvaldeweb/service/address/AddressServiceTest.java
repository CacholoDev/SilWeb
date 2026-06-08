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

    private User sampleUser() {
        return User.builder().id(1L).email("user@example.com").role(com.silvaldeweb.model.user.Role.USER).active(true).build();
    }

    private Address sampleAddress() {
        return Address.builder()
                .id(10L)
                .fullName("John Doe")
                .street("Calle Mayor 1")
                .city("Madrid")
                .province("Madrid")
                .postalCode("28001")
                .country("España")
                .isDefault(true)
                .user(sampleUser())
                .build();
    }

    @Test
    void createSavesAddressWithUser() {
        AddressCreateRequest request = new AddressCreateRequest(
                "John Doe", "Calle Mayor 1", "Madrid", "Madrid", "28001", "España", true, 1L
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser()));
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> {
            Address a = i.getArgument(0);
            a.setId(10L);
            return a;
        });

        AddressResponse response = addressService.create(request);

        assertEquals(10L, response.id());
        assertEquals("John Doe", response.fullName());
        assertEquals("28001", response.postalCode());
        assertEquals(true, response.isDefault());
        assertEquals(1L, response.userId());
    }

    @Test
    void createDefaultsIsDefaultToFalseWhenNull() {
        AddressCreateRequest request = new AddressCreateRequest(
                "Jane", "Calle 2", "Madrid", "Madrid", "28002", "España", null, 1L
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser()));
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> i.getArgument(0));

        AddressResponse response = addressService.create(request);

        assertEquals(false, response.isDefault());
    }

    @Test
    void createThrowsUserNotFoundWhenUserMissing() {
        AddressCreateRequest request = new AddressCreateRequest(
                "John", "Calle 1", "Madrid", "Madrid", "28001", "España", true, 99L
        );
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> addressService.create(request));
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void getThrowsWhenNotFound() {
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AddressNotFoundException.class, () -> addressService.get(99L));
    }

    @Test
    void updateChangesFields() {
        Address existing = sampleAddress();
        when(addressRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> i.getArgument(0));

        AddressUpdateRequest request = new AddressUpdateRequest(
                "Jane Doe", "Calle Nueva 5", "Barcelona", "Barcelona", "08001", "España", false, 1L
        );

        AddressResponse response = addressService.update(10L, request);

        assertEquals("Jane Doe", response.fullName());
        assertEquals("Calle Nueva 5", response.street());
        assertEquals("Barcelona", response.city());
        assertEquals("08001", response.postalCode());
        assertEquals(false, response.isDefault());
    }

    @Test
    void deleteThrowsWhenNotFound() {
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AddressNotFoundException.class, () -> addressService.delete(99L));
    }
}
