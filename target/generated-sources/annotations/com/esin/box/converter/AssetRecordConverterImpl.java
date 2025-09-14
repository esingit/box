package com.esin.box.converter;

import com.esin.box.dto.AssetRecordDTO;
import com.esin.box.entity.AssetRecord;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-09-13T10:30:49+0800",
    comments = "version: 1.5.3.Final, compiler: javac, environment: Java 17.0.16 (Homebrew)"
)
@Component
public class AssetRecordConverterImpl implements AssetRecordConverter {

    @Override
    public AssetRecordDTO toDTO(AssetRecord entity) {
        if ( entity == null ) {
            return null;
        }

        AssetRecordDTO assetRecordDTO = new AssetRecordDTO();

        assetRecordDTO.setVersion( entity.getVersion() );
        assetRecordDTO.setCreateTime( entity.getCreateTime() );
        assetRecordDTO.setUpdateTime( entity.getUpdateTime() );
        assetRecordDTO.setDeleted( entity.getDeleted() );
        assetRecordDTO.setCreateUser( entity.getCreateUser() );
        assetRecordDTO.setUpdateUser( entity.getUpdateUser() );
        assetRecordDTO.setId( entity.getId() );
        assetRecordDTO.setAssetNameId( entity.getAssetNameId() );
        assetRecordDTO.setAssetTypeId( entity.getAssetTypeId() );
        assetRecordDTO.setUnitId( entity.getUnitId() );
        assetRecordDTO.setAssetLocationId( entity.getAssetLocationId() );
        assetRecordDTO.setRemark( entity.getRemark() );
        assetRecordDTO.setAmount( entity.getAmount() );
        assetRecordDTO.setAcquireTime( entity.getAcquireTime() );

        return assetRecordDTO;
    }

    @Override
    public List<AssetRecordDTO> toDTOList(List<AssetRecord> entityList) {
        if ( entityList == null ) {
            return null;
        }

        List<AssetRecordDTO> list = new ArrayList<AssetRecordDTO>( entityList.size() );
        for ( AssetRecord assetRecord : entityList ) {
            list.add( toDTO( assetRecord ) );
        }

        return list;
    }
}
