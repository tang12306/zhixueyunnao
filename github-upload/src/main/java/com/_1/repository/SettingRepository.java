package com._1.repository;

import com._1.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettingRepository extends JpaRepository<Setting, String> {
    List<Setting> findByIsEditable(boolean isEditable);
} 