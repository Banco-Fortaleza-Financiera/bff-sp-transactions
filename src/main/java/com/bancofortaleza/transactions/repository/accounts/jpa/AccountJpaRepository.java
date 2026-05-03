package com.bancofortaleza.transactions.repository.accounts.jpa;

import com.bancofortaleza.transactions.repository.accounts.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, Integer> {
}
