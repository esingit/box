package com.esin.box.converter;

import com.esin.box.dto.FitnessRecordDTO;
import com.esin.box.entity.FitnessRecord;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring") // 让它能被 Spring 扫描
public interface FitnessRecordConverter {

    FitnessRecordDTO toDTO(FitnessRecord entity);

    List<FitnessRecordDTO> toDTOList(List<FitnessRecord> entityList);
}
