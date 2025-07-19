package com.example.ainovel.repository;

import com.example.ainovel.model.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    Optional<UserSetting> findByUserId(Long userId);
}
