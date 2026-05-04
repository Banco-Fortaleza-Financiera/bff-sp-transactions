package com.bancofortaleza.transactions.repository.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancofortaleza.transactions.repository.accounts.entity.AccountEntity;
import com.bancofortaleza.transactions.repository.accounts.jpa.AccountJpaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountRepositoryTest {

    @Mock
    private AccountJpaRepository accountJpaRepository;

    @InjectMocks
    private AccountRepository accountRepository;

    @Test
    void getAccountByIdShouldDelegateToJpaRepository() {
        // Arrange
        AccountEntity account = new AccountEntity();
        when(accountJpaRepository.findById(1)).thenReturn(Optional.of(account));

        // Act
        Optional<AccountEntity> result = accountRepository.getAccountById(1);

        // Assert
        assertThat(result).contains(account);
    }

    @Test
    void findAccountsByUserShouldDelegateToJpaRepository() {
        // Arrange
        AccountEntity account = new AccountEntity();
        when(accountJpaRepository.findByIdUser(10)).thenReturn(List.of(account));

        // Act
        List<AccountEntity> result = accountRepository.findAccountsByUser(10);

        // Assert
        assertThat(result).containsExactly(account);
    }

    @Test
    void saveShouldDelegateToJpaRepository() {
        // Arrange
        AccountEntity account = new AccountEntity();
        when(accountJpaRepository.save(account)).thenReturn(account);

        // Act
        AccountEntity result = accountRepository.save(account);

        // Assert
        assertThat(result).isSameAs(account);
        verify(accountJpaRepository).save(account);
    }
}
