package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Unsae;

@Repository
public interface UnsaeRepository extends JpaRepository<Unsae, Long>{
    
}
