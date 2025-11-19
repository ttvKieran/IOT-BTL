package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {

    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deletedAt = FUNCTION('CURRENT_TIMESTAMP') WHERE e.id in :ids")
    void softDeleteByIds(List<ID> ids);

    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deletedAt = null WHERE e.id = :id")
    void restoreById(@Param("id") ID id);
}
