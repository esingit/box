package com.esin.box.converter;

import com.esin.box.dto.FitnessRecordDTO;
import com.esin.box.entity.FitnessRecord;
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
public class FitnessRecordConverterImpl implements FitnessRecordConverter {

    @Override
    public FitnessRecordDTO toDTO(FitnessRecord entity) {
        if ( entity == null ) {
            return null;
        }

        FitnessRecordDTO fitnessRecordDTO = new FitnessRecordDTO();

        fitnessRecordDTO.setVersion( entity.getVersion() );
        fitnessRecordDTO.setCreateTime( entity.getCreateTime() );
        fitnessRecordDTO.setUpdateTime( entity.getUpdateTime() );
        fitnessRecordDTO.setDeleted( entity.getDeleted() );
        fitnessRecordDTO.setCreateUser( entity.getCreateUser() );
        fitnessRecordDTO.setUpdateUser( entity.getUpdateUser() );
        fitnessRecordDTO.setId( entity.getId() );
        fitnessRecordDTO.setTypeId( entity.getTypeId() );
        fitnessRecordDTO.setCount( entity.getCount() );
        fitnessRecordDTO.setUnitId( entity.getUnitId() );
        fitnessRecordDTO.setFinishTime( entity.getFinishTime() );
        fitnessRecordDTO.setRemark( entity.getRemark() );

        return fitnessRecordDTO;
    }

    @Override
    public List<FitnessRecordDTO> toDTOList(List<FitnessRecord> entityList) {
        if ( entityList == null ) {
            return null;
        }

        List<FitnessRecordDTO> list = new ArrayList<FitnessRecordDTO>( entityList.size() );
        for ( FitnessRecord fitnessRecord : entityList ) {
            list.add( toDTO( fitnessRecord ) );
        }

        return list;
    }
}
