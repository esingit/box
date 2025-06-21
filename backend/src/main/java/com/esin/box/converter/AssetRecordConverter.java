package com.esin.box.converter;

import com.esin.box.dto.AssetRecordDTO;
import com.esin.box.entity.AssetRecord;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AssetRecordConverter {
    AssetRecordDTO toDTO(AssetRecord entity);

    List<AssetRecordDTO> toDTOList(List<AssetRecord> entityList);
}