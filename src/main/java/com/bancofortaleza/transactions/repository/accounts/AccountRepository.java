package com.bancofortaleza.transactions.repository.accounts;

import com.bancofortaleza.transactions.repository.accounts.entity.AccountEntity;
import com.bancofortaleza.transactions.repository.accounts.jpa.AccountJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountRepository {

    private final AccountJpaRepository accountJpaRepository;

    public Optional<AccountEntity> getAccountById(Integer id) {
        return accountJpaRepository.findById(id);
    }

    public List<AccountEntity> findAccountsByUser(Integer idUser) {
        return accountJpaRepository.findByIdUser(idUser);
    }

    public AccountEntity save(AccountEntity account) {
        return accountJpaRepository.save(account);
    }
}
