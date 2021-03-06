package com.thoughtworks.rslist.repository;

import com.thoughtworks.rslist.dto.RsEventDto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RsEventRepository extends CrudRepository<RsEventDto, Integer> {
  List<RsEventDto> findAll();

  @Transactional
  void deleteAllByUserId(int userId);
  @Transactional
  void deleteByRank(int rank);

  RsEventDto findByRank(int rank);
}
