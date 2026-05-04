package com.bancofortaleza.transactions.repository.accounts.jpa;

import com.bancofortaleza.transactions.repository.accounts.entity.AccountEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, Integer> {

    List<AccountEntity> findByIdUser(Integer idUser);
}
